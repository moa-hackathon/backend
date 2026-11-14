package com.moa.backend.notice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.backend.ai.application.port.in.GenerateTextUseCase;
import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.notice.dto.NoticeChatField;
import com.moa.backend.notice.dto.NoticeChatMessage;
import com.moa.backend.notice.dto.NoticeChatRequest;
import com.moa.backend.notice.dto.NoticeChatResponse;
import com.moa.backend.notice.dto.NoticeChatRole;
import com.moa.backend.notice.dto.NoticeChatSummary;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 공지 입력 · 되묻기 (F1). 지금까지의 대화를 받아 행사 요약을 정리하고 빠진 항목을 되묻는다.
 * 서버는 대화를 저장하지 않는다.
 */
@Slf4j
@Service
public class NoticeChatService {

	static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	static final int MAX_ATTEMPTS = 2;
	static final List<String> DEFAULT_SUGGESTIONS = List.of("뒤풀이 참석 여부", "알레르기");

	private final GenerateTextUseCase generateText;
	private final Clock clock;
	private final NoticeChatPrompt prompt;
	private final NoticeChatResponseParser parser;

	@Autowired
	public NoticeChatService(GenerateTextUseCase generateText) {
		this(generateText, Clock.system(SEOUL));
	}

	NoticeChatService(GenerateTextUseCase generateText, Clock clock) {
		ObjectMapper objectMapper = new ObjectMapper();
		this.generateText = generateText;
		this.clock = clock;
		this.prompt = new NoticeChatPrompt(objectMapper);
		this.parser = new NoticeChatResponseParser(objectMapper);
	}

	public NoticeChatResponse chat(Club club, NoticeChatRequest request) {
		List<NoticeChatMessage> messages = request.messages();
		if (messages.get(messages.size() - 1).role() != NoticeChatRole.USER) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "마지막 메시지는 운영진(user) 메시지여야 합니다.");
		}
		NoticeChatSummary previous = sanitize(request.summary());
		List<String> selected = NoticeChatResponseParser.cleanList(
				request.selectedExtraQuestions(), NoticeChatSummary.MAX_EXTRA_QUESTIONS);

		LocalDate today = LocalDate.ofInstant(clock.instant(), SEOUL);
		String systemPrompt = prompt.system(today, club.getName());
		String userPrompt = prompt.user(club.getName(), previous, selected, messages);

		NoticeChatModelOutput output = ask(systemPrompt, userPrompt);
		NoticeChatSummary summary = merge(previous, output, selected);
		List<NoticeChatField> missing = summary.missingFields();
		return new NoticeChatResponse(
				reply(output.reply(), missing),
				summary,
				missing,
				suggestions(output.suggestedExtraQuestions(), summary.extraQuestions()),
				missing.isEmpty());
	}

	/** 답이 잘렸거나 JSON으로 읽히지 않으면 한 번 더 묻고, 그래도 안 되면 실패로 돌려준다. */
	private NoticeChatModelOutput ask(String systemPrompt, String userPrompt) {
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			String prompt = attempt == 1 ? userPrompt : userPrompt + NoticeChatPrompt.RETRY_INSTRUCTION;
			AiCompletion completion;
			try {
				completion = generateText.generate(null, systemPrompt, prompt);
			} catch (RuntimeException e) {
				log.warn("공지 채팅 AI 호출 실패 ({}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
				continue;
			}
			if (completion == null || completion.truncated()) {
				log.warn("공지 채팅 AI 답변이 비었거나 잘렸습니다 ({}/{})", attempt, MAX_ATTEMPTS);
				continue;
			}
			Optional<NoticeChatModelOutput> output = parser.parse(completion.text());
			if (output.isPresent()) {
				return output.get();
			}
			log.warn("공지 채팅 AI 답변을 JSON으로 읽지 못했습니다 ({}/{})", attempt, MAX_ATTEMPTS);
		}
		throw new ApiException(ErrorCode.AI_RESPONSE_FAILED);
	}

	/** 클라이언트가 돌려보낸 요약도 모델 답과 같은 기준으로 다듬는다. */
	private static NoticeChatSummary sanitize(NoticeChatSummary summary) {
		if (summary == null) {
			return NoticeChatSummary.empty();
		}
		return new NoticeChatSummary(
				NoticeChatResponseParser.cleanText(summary.title(), NoticeChatSummary.MAX_TITLE_LENGTH),
				minutes(summary.eventAt()),
				NoticeChatResponseParser.cleanText(summary.location(), NoticeChatSummary.MAX_LOCATION_LENGTH),
				NoticeChatResponseParser.validPeople(summary.capacity()),
				NoticeChatResponseParser.validPeople(summary.minCapacity()),
				minutes(summary.applyDeadline()),
				NoticeChatResponseParser.cleanList(summary.extraQuestions(), NoticeChatSummary.MAX_EXTRA_QUESTIONS),
				NoticeChatResponseParser.cleanText(summary.description(), NoticeChatSummary.MAX_DESCRIPTION_LENGTH));
	}

	/**
	 * 모델이 준 값을 우선하되, 모델이 비워 둔 값은 이전 요약 값을 지킨다 (모델이 실수로 값을 지워도 잃지 않게).
	 * 고른 추가 질문은 모델 답과 관계없이 넣는다.
	 */
	static NoticeChatSummary merge(NoticeChatSummary previous, NoticeChatModelOutput output, List<String> selected) {
		List<String> extraQuestions = new ArrayList<>(
				output.extraQuestions() != null ? output.extraQuestions() : previous.extraQuestions());
		extraQuestions.addAll(selected);
		return new NoticeChatSummary(
				firstNonNull(output.title(), previous.title()),
				firstNonNull(output.eventAt(), previous.eventAt()),
				firstNonNull(output.location(), previous.location()),
				firstNonNull(output.capacity(), previous.capacity()),
				firstNonNull(output.minCapacity(), previous.minCapacity()),
				firstNonNull(output.applyDeadline(), previous.applyDeadline()),
				NoticeChatResponseParser.cleanList(extraQuestions, NoticeChatSummary.MAX_EXTRA_QUESTIONS),
				firstNonNull(output.description(), previous.description()));
	}

	static String reply(String modelReply, List<NoticeChatField> missing) {
		if (modelReply != null) {
			return modelReply;
		}
		if (missing.isEmpty()) {
			return "이렇게 정리했어요.";
		}
		return "아래 항목만 더 알려 주세요: "
				+ missing.stream().map(NoticeChatField::getLabel).collect(Collectors.joining(", "));
	}

	static List<String> suggestions(List<String> modelSuggestions, List<String> extraQuestions) {
		List<String> source = modelSuggestions == null || modelSuggestions.isEmpty()
				? DEFAULT_SUGGESTIONS
				: modelSuggestions;
		return source.stream()
				.filter(s -> !extraQuestions.contains(s))
				.limit(NoticeChatResponseParser.MAX_SUGGESTIONS)
				.toList();
	}

	private static LocalDateTime minutes(LocalDateTime value) {
		return value == null ? null : value.truncatedTo(ChronoUnit.MINUTES);
	}

	private static <T> T firstNonNull(T first, T second) {
		return first != null ? first : second;
	}
}
