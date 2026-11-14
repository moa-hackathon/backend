package com.moa.backend.notice.service;

import com.moa.backend.ai.application.port.in.GenerateTextUseCase;
import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.domain.EventDetails;
import com.moa.backend.notice.domain.FormLinkPlaceholder;
import com.moa.backend.notice.domain.NoticeFacts;
import com.moa.backend.notice.dto.NoticeDraftRequest;
import com.moa.backend.notice.dto.NoticeDraftResponse;
import com.moa.backend.notice.dto.NoticeDraftSource;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 행사 요약으로 공지 초안(F2)을 만든다.
 * <ul>
 *   <li>사실(일시, 장소, 정원, 신청 마감)은 요약에서 정해진 형식으로 만들어 넘기고, AI 초안에 빠졌으면 쓰지 않는다.</li>
 *   <li>신청 링크 자리는 코드에서 {@code [구글폼 링크]}로 맞춘다. 모델이 빼먹거나 링크를 지어내도 바로잡는다.</li>
 *   <li>처음 만들 때 AI 응답을 쓸 수 없으면 기본 양식으로 만든다. 수정 요청은 양식으로 대신하면 요청이 무시되므로 502로 알린다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeDraftService {

	static final int PREVIOUS_DRAFT_MAX_LENGTH = 5000;
	static final int REVISION_REQUEST_MAX_LENGTH = 500;

	private static final Pattern CODE_FENCE = Pattern.compile("^```[a-zA-Z]*\\s*|\\s*```$");

	private final GenerateTextUseCase generateTextUseCase;

	public NoticeDraftResponse draft(Club club, NoticeDraftRequest request) {
		EventDetails details = request.toDetails();
		NoticeFacts facts = NoticeFacts.of(club.getName(), details);

		if (!request.isRevision()) {
			String generated = generate(NoticeDraftPrompts.SYSTEM, NoticeDraftPrompts.draft(facts, details), facts);
			if (generated != null) {
				return new NoticeDraftResponse(generated, NoticeDraftSource.AI);
			}
			return new NoticeDraftResponse(facts.renderTemplate(), NoticeDraftSource.TEMPLATE);
		}

		String previousDraft = request.previousDraft();
		if (previousDraft == null || previousDraft.isBlank()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "수정할 이전 초안이 필요합니다.");
		}
		if (previousDraft.length() > PREVIOUS_DRAFT_MAX_LENGTH) {
			throw new ApiException(ErrorCode.INVALID_REQUEST,
					"이전 초안은 " + PREVIOUS_DRAFT_MAX_LENGTH + "자 이하여야 합니다.");
		}
		String revisionRequest = request.revisionRequest().trim();
		if (revisionRequest.length() > REVISION_REQUEST_MAX_LENGTH) {
			throw new ApiException(ErrorCode.INVALID_REQUEST,
					"수정 요청은 " + REVISION_REQUEST_MAX_LENGTH + "자 이하로 입력해 주세요.");
		}
		String revised = generate(NoticeDraftPrompts.SYSTEM,
				NoticeDraftPrompts.revision(facts, details, previousDraft.strip(), revisionRequest), facts);
		if (revised == null) {
			throw new ApiException(ErrorCode.AI_RESPONSE_FAILED, "수정 요청을 반영하지 못했어요. 다시 시도해 주세요.");
		}
		return new NoticeDraftResponse(revised, NoticeDraftSource.AI);
	}

	/** 쓸 수 있는 AI 초안을 돌려준다. 호출 실패, 잘린 응답, 빈 응답, 사실이 빠진 응답이면 null. */
	private String generate(String systemPrompt, String userPrompt, NoticeFacts facts) {
		AiCompletion completion;
		try {
			completion = generateTextUseCase.generate(null, systemPrompt, userPrompt);
		} catch (RuntimeException e) {
			log.warn("공지 초안 AI 호출 실패: {}", e.getMessage());
			return null;
		}
		if (completion == null || completion.text() == null || completion.text().isBlank()) {
			log.warn("공지 초안 AI 응답이 비어 있음");
			return null;
		}
		if (completion.truncated()) {
			log.warn("공지 초안 AI 응답이 길이 한도로 잘림");
			return null;
		}
		String text = CODE_FENCE.matcher(completion.text().strip()).replaceAll("").strip();
		if (text.isEmpty()) {
			return null;
		}
		if (!facts.coveredBy(text)) {
			log.warn("공지 초안 AI 응답에 행사 정보가 빠짐");
			return null;
		}
		return FormLinkPlaceholder.ensure(text);
	}
}
