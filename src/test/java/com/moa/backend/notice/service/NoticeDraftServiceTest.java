package com.moa.backend.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moa.backend.ai.application.port.in.GenerateTextUseCase;
import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.ai.domain.AiProvider;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.notice.dto.NoticeDraftRequest;
import com.moa.backend.notice.dto.NoticeDraftResponse;
import com.moa.backend.notice.dto.NoticeDraftSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class NoticeDraftServiceTest {

	private static final String AI_DRAFT = """
			[모아] 네트워킹 행사 안내
			일시: 11/21(토) 19:00
			장소: 공학관 302호
			정원: 40명 (선착순)
			신청 마감: 11/19(목)
			피자가 준비되어 있어요!
			신청 링크: [구글폼 링크]""";

	private static final String TEMPLATE = """
			[모아] 네트워킹 행사 안내
			일시: 11/21(토) 19:00
			장소: 공학관 302호
			정원: 40명 (선착순)
			신청 마감: 11/19(목)
			피자 나와요
			신청 링크: [구글폼 링크]""";

	private final Club club = Club.builder().name("모아").inviteCode("code").build();

	@Test
	void AI가_쓴_초안을_돌려주고_프롬프트에_요약의_사실을_넣는다() {
		FakeGenerateText ai = FakeGenerateText.returning(AI_DRAFT);

		NoticeDraftResponse response = new NoticeDraftService(ai).draft(club, request(null, null));

		assertThat(response.source()).isEqualTo(NoticeDraftSource.AI);
		assertThat(response.noticeText()).isEqualTo(AI_DRAFT);
		assertThat(ai.calls).hasSize(1);
		assertThat(ai.calls.get(0).provider()).isNull();
		assertThat(ai.calls.get(0).systemPrompt()).contains("신청 링크: [구글폼 링크]");
		assertThat(ai.calls.get(0).userPrompt())
				.contains("[모아] 네트워킹 행사 안내", "일시: 11/21(토) 19:00", "장소: 공학관 302호",
						"정원: 40명 (선착순)", "신청 마감: 11/19(목)", "피자 나와요")
				.doesNotContain("[이전 초안]");
	}

	@Test
	void AI가_신청_링크_자리를_빼먹으면_붙인다() {
		String withoutLink = AI_DRAFT.replace("\n신청 링크: [구글폼 링크]", "");

		NoticeDraftResponse response = new NoticeDraftService(FakeGenerateText.returning(withoutLink))
				.draft(club, request(null, null));

		assertThat(response.source()).isEqualTo(NoticeDraftSource.AI);
		assertThat(response.noticeText()).isEqualTo(AI_DRAFT);
	}

	@Test
	void AI가_링크를_지어내면_자리_표시로_바꾸고_코드_블록을_벗긴다() {
		String fenced = "```\n" + AI_DRAFT.replace("[구글폼 링크]", "https://forms.gle/fake") + "\n```";

		NoticeDraftResponse response = new NoticeDraftService(FakeGenerateText.returning(fenced))
				.draft(club, request(null, null));

		assertThat(response.noticeText()).isEqualTo(AI_DRAFT);
	}

	@Test
	void AI_호출이_실패하면_기본_양식으로_만든다() {
		GenerateTextUseCase failing = (provider, system, user) -> {
			throw new IllegalStateException("API 키 없음");
		};

		NoticeDraftResponse response = new NoticeDraftService(failing).draft(club, request(null, null));

		assertThat(response.source()).isEqualTo(NoticeDraftSource.TEMPLATE);
		assertThat(response.noticeText()).isEqualTo(TEMPLATE);
	}

	@Test
	void AI_응답이_잘리거나_비면_기본_양식으로_만든다() {
		FakeGenerateText truncated = new FakeGenerateText(prompt -> completion(AI_DRAFT, true));
		FakeGenerateText blank = FakeGenerateText.returning("  ");

		assertThat(new NoticeDraftService(truncated).draft(club, request(null, null)).source())
				.isEqualTo(NoticeDraftSource.TEMPLATE);
		assertThat(new NoticeDraftService(blank).draft(club, request(null, null)).source())
				.isEqualTo(NoticeDraftSource.TEMPLATE);
	}

	@Test
	void AI가_행사_정보를_바꾸거나_빠뜨리면_기본_양식으로_만든다() {
		String wrongPlace = AI_DRAFT.replace("공학관 302호", "학생회관 101호");

		NoticeDraftResponse response = new NoticeDraftService(FakeGenerateText.returning(wrongPlace))
				.draft(club, request(null, null));

		assertThat(response.source()).isEqualTo(NoticeDraftSource.TEMPLATE);
		assertThat(response.noticeText()).isEqualTo(TEMPLATE);
	}

	@Test
	void 수정_요청이면_이전_초안과_요청을_프롬프트에_넣는다() {
		String revised = AI_DRAFT.replace("피자가 준비되어 있어요!", "맛있는 피자가 기다려요 🍕");
		FakeGenerateText ai = FakeGenerateText.returning(revised);

		NoticeDraftResponse response = new NoticeDraftService(ai).draft(club, request(AI_DRAFT, " 더 친근하게 "));

		assertThat(response.source()).isEqualTo(NoticeDraftSource.AI);
		assertThat(response.noticeText()).isEqualTo(revised);
		assertThat(ai.calls.get(0).userPrompt())
				.contains("[이전 초안]\n" + AI_DRAFT, "[수정 요청]\n더 친근하게", "일시: 11/21(토) 19:00");
	}

	@Test
	void 수정_요청이_실패하면_양식으로_대신하지_않고_502() {
		GenerateTextUseCase failing = (provider, system, user) -> {
			throw new IllegalStateException("timeout");
		};

		assertThatThrownBy(() -> new NoticeDraftService(failing).draft(club, request(AI_DRAFT, "더 짧게")))
				.isInstanceOf(ApiException.class)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.AI_RESPONSE_FAILED);
	}

	@Test
	void 수정_요청에_이전_초안이_없으면_400() {
		FakeGenerateText ai = FakeGenerateText.returning(AI_DRAFT);

		assertThatThrownBy(() -> new NoticeDraftService(ai).draft(club, request(" ", "더 짧게")))
				.isInstanceOf(ApiException.class)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
		assertThat(ai.calls).isEmpty();
	}

	@Test
	void 요약이_규칙을_어기면_AI를_부르지_않고_400() {
		FakeGenerateText ai = FakeGenerateText.returning(AI_DRAFT);
		NoticeDraftRequest invalid = new NoticeDraftRequest("행사", LocalDateTime.of(2026, 11, 21, 19, 0), "장소",
				10, 11, LocalDateTime.of(2026, 11, 19, 23, 59), null, null, null, null);

		assertThatThrownBy(() -> new NoticeDraftService(ai).draft(club, invalid))
				.isInstanceOf(ApiException.class)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
		assertThat(ai.calls).isEmpty();
	}

	private static NoticeDraftRequest request(String previousDraft, String revisionRequest) {
		return new NoticeDraftRequest("모아 네트워킹 행사", LocalDateTime.of(2026, 11, 21, 19, 0), "공학관 302호", 40, 20,
				LocalDateTime.of(2026, 11, 19, 23, 59), List.of("뒤풀이 참석 여부"), "피자 나와요", previousDraft,
				revisionRequest);
	}

	private static AiCompletion completion(String text, boolean truncated) {
		return new AiCompletion(text, AiProvider.UPSTAGE, "fake", truncated, null, null);
	}

	private record Call(AiProvider provider, String systemPrompt, String userPrompt) {
	}

	private static final class FakeGenerateText implements GenerateTextUseCase {

		private final Function<Call, AiCompletion> responder;
		private final List<Call> calls = new ArrayList<>();

		private FakeGenerateText(Function<Call, AiCompletion> responder) {
			this.responder = responder;
		}

		static FakeGenerateText returning(String text) {
			return new FakeGenerateText(call -> completion(text, false));
		}

		@Override
		public AiCompletion generate(AiProvider provider, String systemPrompt, String userPrompt) {
			Call call = new Call(provider, systemPrompt, userPrompt);
			calls.add(call);
			return responder.apply(call);
		}
	}
}
