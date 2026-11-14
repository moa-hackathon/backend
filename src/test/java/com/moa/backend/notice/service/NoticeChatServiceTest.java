package com.moa.backend.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moa.backend.ai.application.port.in.GenerateTextUseCase;
import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.ai.domain.AiProvider;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoticeChatServiceTest {

	/** 2026-11-14(토) 10:00, 서울. */
	private static final Clock CLOCK = Clock.fixed(
			ZonedDateTime.of(2026, 11, 14, 10, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("UTC"));

	private static final Club CLUB = Club.builder().name("모아").inviteCode("invite").build();

	private static final String FIRST_TURN_JSON = """
			{"reply": "거의 다 됐어요. 장소, 정원, 신청 마감일을 알려 주세요.",
			 "summary": {"title": "모아 네트워킹 행사", "eventAt": "2026-11-20T19:00", "location": null,
			             "capacity": null, "minCapacity": 20, "applyDeadline": null,
			             "extraQuestions": [], "description": null},
			 "suggestedExtraQuestions": ["뒤풀이 참석 여부", "알레르기"]}
			""";

	private final FakeGenerateText fake = new FakeGenerateText();
	private final NoticeChatService service = new NoticeChatService(fake, CLOCK);

	@Test
	void 대화를_행사_요약으로_정리하고_빠진_항목을_알려준다() {
		fake.reply(FIRST_TURN_JSON);

		NoticeChatResponse response = service.chat(CLUB, request(null, null,
				user("다음 주 금요일 저녁 7시에 네트워킹 행사 해요. 20명은 넘어야 진행해요.")));

		assertThat(response.reply()).isEqualTo("거의 다 됐어요. 장소, 정원, 신청 마감일을 알려 주세요.");
		NoticeChatSummary summary = response.summary();
		assertThat(summary.title()).isEqualTo("모아 네트워킹 행사");
		assertThat(summary.eventAt()).isEqualTo(LocalDateTime.of(2026, 11, 20, 19, 0));
		assertThat(summary.location()).isNull();
		assertThat(summary.capacity()).isNull();
		assertThat(summary.minCapacity()).isEqualTo(20);
		assertThat(summary.applyDeadline()).isNull();
		assertThat(summary.extraQuestions()).isEmpty();
		assertThat(response.missingFields()).containsExactly(
				NoticeChatField.LOCATION, NoticeChatField.CAPACITY, NoticeChatField.APPLY_DEADLINE);
		assertThat(response.suggestedExtraQuestions()).containsExactly("뒤풀이 참석 여부", "알레르기");
		assertThat(response.complete()).isFalse();
	}

	@Test
	void 프롬프트에_서울_기준_오늘과_달력과_대화를_넣는다() {
		fake.reply(FIRST_TURN_JSON);

		service.chat(CLUB, request(null, null, user("다음 주 금요일 저녁 7시에 네트워킹 행사 해요.")));

		FakeGenerateText.Call call = fake.calls.get(0);
		assertThat(call.provider()).isNull();
		assertThat(call.systemPrompt())
				.contains("2026-11-14(토), 시간대 Asia/Seoul")
				.contains("- 다음 주: 2026-11-16(월)")
				.contains("2026-11-20(금)")
				.contains("절대 지어내지 않는다");
		assertThat(call.userPrompt())
				.contains("동아리: 모아")
				.contains("\"role\":\"user\"")
				.contains("다음 주 금요일 저녁 7시에 네트워킹 행사 해요.");
	}

	@Test
	void 코드블록과_앞뒤_설명이_붙어도_읽는다() {
		fake.reply("네, 정리했어요.\n```json\n" + FIRST_TURN_JSON + "\n```\n");

		NoticeChatResponse response = service.chat(CLUB, request(null, null, user("네트워킹 행사 해요")));

		assertThat(response.summary().title()).isEqualTo("모아 네트워킹 행사");
	}

	@Test
	void 이전_요약을_유지하고_고른_추가_질문을_넣으면_완료된다() {
		NoticeChatSummary previous = new NoticeChatSummary("모아 네트워킹 행사", LocalDateTime.of(2026, 11, 20, 19, 0),
				null, null, 20, null, List.of(), null);
		// 모델이 이전 값을 빠뜨려도 지켜야 한다
		fake.reply("""
				{"reply": "이렇게 정리했어요.",
				 "summary": {"title": null, "eventAt": null, "location": "공학관 302호", "capacity": "40명",
				             "minCapacity": null, "applyDeadline": "2026-11-18", "extraQuestions": null,
				             "description": "피자가 준비되어 있어요."},
				 "suggestedExtraQuestions": ["뒤풀이 참석 여부", "알레르기", "티셔츠 사이즈"]}
				""");

		NoticeChatResponse response = service.chat(CLUB, request(previous, List.of("뒤풀이 참석 여부"),
				user("다음 주 금요일 저녁 7시에 네트워킹 행사 해요. 20명은 넘어야 진행해요."),
				assistant("거의 다 됐어요. 아래 항목만 더 알려 주세요."),
				user("공학관 302호, 정원 40명, 신청은 18일까지. 피자 나와요.")));

		NoticeChatSummary summary = response.summary();
		assertThat(summary.title()).isEqualTo("모아 네트워킹 행사");
		assertThat(summary.eventAt()).isEqualTo(LocalDateTime.of(2026, 11, 20, 19, 0));
		assertThat(summary.location()).isEqualTo("공학관 302호");
		assertThat(summary.capacity()).isEqualTo(40);
		assertThat(summary.minCapacity()).isEqualTo(20);
		assertThat(summary.applyDeadline()).isEqualTo(LocalDateTime.of(2026, 11, 18, 23, 59));
		assertThat(summary.extraQuestions()).containsExactly("뒤풀이 참석 여부");
		assertThat(summary.description()).isEqualTo("피자가 준비되어 있어요.");
		assertThat(response.missingFields()).isEmpty();
		assertThat(response.complete()).isTrue();
		assertThat(response.suggestedExtraQuestions()).containsExactly("알레르기", "티셔츠 사이즈");
		assertThat(fake.calls.get(0).userPrompt())
				.contains("\"minCapacity\":20")
				.contains("\"eventAt\":\"2026-11-20T19:00\"")
				.contains("[\"뒤풀이 참석 여부\"]");
	}

	@Test
	void 맞지_않는_값은_null로_둔다() {
		fake.reply("""
				{"reply": "", "summary": {"title": "  ", "eventAt": "2026-11-20", "location": "null",
				  "capacity": -3, "minCapacity": "20~30명", "applyDeadline": "다음 주 수요일", "extraQuestions": "뒤풀이"},
				 "suggestedExtraQuestions": []}
				""");

		NoticeChatResponse response = service.chat(CLUB, request(null, null, user("행사 해요")));

		NoticeChatSummary summary = response.summary();
		assertThat(summary.title()).isNull();
		assertThat(summary.eventAt()).isNull();
		assertThat(summary.location()).isNull();
		assertThat(summary.capacity()).isNull();
		assertThat(summary.minCapacity()).isNull();
		assertThat(summary.applyDeadline()).isNull();
		assertThat(summary.extraQuestions()).isEmpty();
		assertThat(response.missingFields()).hasSize(6);
		assertThat(response.reply()).isEqualTo("아래 항목만 더 알려 주세요: 행사명, 일시, 장소, 정원, 최소 필수 인원, 신청 마감일");
		assertThat(response.suggestedExtraQuestions()).isEqualTo(NoticeChatService.DEFAULT_SUGGESTIONS);
	}

	@Test
	void JSON이_아니면_한_번_더_묻는다() {
		fake.reply("죄송해요, 다시 말씀해 주세요.");
		fake.reply(FIRST_TURN_JSON);

		NoticeChatResponse response = service.chat(CLUB, request(null, null, user("네트워킹 행사 해요")));

		assertThat(response.summary().title()).isEqualTo("모아 네트워킹 행사");
		assertThat(fake.calls).hasSize(2);
		assertThat(fake.calls.get(1).userPrompt()).endsWith(NoticeChatPrompt.RETRY_INSTRUCTION);
	}

	@Test
	void 답이_잘리면_한_번_더_묻는다() {
		fake.replyTruncated("{\"reply\": \"거의 다");
		fake.reply(FIRST_TURN_JSON);

		NoticeChatResponse response = service.chat(CLUB, request(null, null, user("네트워킹 행사 해요")));

		assertThat(response.summary().minCapacity()).isEqualTo(20);
		assertThat(fake.calls).hasSize(2);
	}

	@Test
	void 두_번_모두_실패하면_AI_RESPONSE_FAILED() {
		fake.reply("{ 깨진 json");
		fake.fail(new IllegalStateException("Upstage API 호출에 실패했습니다"));

		assertThatThrownBy(() -> service.chat(CLUB, request(null, null, user("네트워킹 행사 해요"))))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_FAILED));
		assertThat(fake.calls).hasSize(NoticeChatService.MAX_ATTEMPTS);
	}

	@Test
	void 마지막_메시지가_운영진_메시지가_아니면_400() {
		assertThatThrownBy(() -> service.chat(CLUB, request(null, null,
				user("네트워킹 행사 해요"), assistant("장소를 알려 주세요."))))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
		assertThat(fake.calls).isEmpty();
	}

	private static NoticeChatRequest request(NoticeChatSummary summary, List<String> selected,
			NoticeChatMessage... messages) {
		return new NoticeChatRequest(List.of(messages), summary, selected);
	}

	private static NoticeChatMessage user(String content) {
		return new NoticeChatMessage(NoticeChatRole.USER, content);
	}

	private static NoticeChatMessage assistant(String content) {
		return new NoticeChatMessage(NoticeChatRole.ASSISTANT, content);
	}

	/** 정해 둔 답을 차례로 돌려주는 가짜 LLM. */
	private static class FakeGenerateText implements GenerateTextUseCase {

		record Call(AiProvider provider, String systemPrompt, String userPrompt) {
		}

		private final Deque<Object> answers = new ArrayDeque<>();
		private final List<Call> calls = new ArrayList<>();

		void reply(String text) {
			answers.add(new AiCompletion(text, AiProvider.UPSTAGE, "fake", false, null, null));
		}

		void replyTruncated(String text) {
			answers.add(new AiCompletion(text, AiProvider.UPSTAGE, "fake", true, null, null));
		}

		void fail(RuntimeException e) {
			answers.add(e);
		}

		@Override
		public AiCompletion generate(AiProvider provider, String systemPrompt, String userPrompt) {
			calls.add(new Call(provider, systemPrompt, userPrompt));
			Object answer = answers.poll();
			if (answer instanceof RuntimeException e) {
				throw e;
			}
			if (answer == null) {
				throw new IllegalStateException("준비된 답이 없습니다.");
			}
			return (AiCompletion) answer;
		}
	}
}
