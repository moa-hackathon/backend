package com.moa.backend.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventDetailsTest {

	private static final LocalDateTime EVENT_AT = LocalDateTime.of(2026, 11, 21, 19, 0);
	private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 11, 19, 23, 59);

	@Test
	void 문자열을_다듬고_추가_질문의_빈_항목과_중복을_뺀다() {
		EventDetails details = EventDetails.of("  모아 네트워킹 행사 ", EVENT_AT, " 공학관 302호", 40, 20, DEADLINE,
				Arrays.asList(" 뒤풀이 참석 여부", "", null, "뒤풀이 참석 여부", "알레르기"), "  ");

		assertThat(details.title()).isEqualTo("모아 네트워킹 행사");
		assertThat(details.location()).isEqualTo("공학관 302호");
		assertThat(details.extraQuestions()).containsExactly("뒤풀이 참석 여부", "알레르기");
		assertThat(details.description()).isNull();
	}

	@Test
	void 최소_인원과_추가_질문은_생략할_수_있다() {
		EventDetails details = EventDetails.of("행사", EVENT_AT, "장소", 1, null, EVENT_AT, null, null);

		assertThat(details.minCapacity()).isZero();
		assertThat(details.extraQuestions()).isEmpty();
	}

	@Test
	void 최소_인원이_정원과_같아도_된다() {
		assertThat(EventDetails.of("행사", EVENT_AT, "장소", 40, 40, DEADLINE, List.of(), null).minCapacity())
				.isEqualTo(40);
	}

	@Test
	void 정원은_1명_이상이어야_한다() {
		assertInvalid(() -> EventDetails.of("행사", EVENT_AT, "장소", 0, 0, DEADLINE, null, null), "정원");
	}

	@Test
	void 최소_인원은_정원을_넘을_수_없다() {
		assertInvalid(() -> EventDetails.of("행사", EVENT_AT, "장소", 40, 41, DEADLINE, null, null), "최소 인원");
		assertInvalid(() -> EventDetails.of("행사", EVENT_AT, "장소", 40, -1, DEADLINE, null, null), "최소 인원");
	}

	@Test
	void 신청_마감은_행사_일시보다_늦을_수_없다() {
		assertInvalid(() -> EventDetails.of("행사", EVENT_AT, "장소", 40, 20, EVENT_AT.plusMinutes(1), null, null),
				"신청 마감");
	}

	@Test
	void 필수_항목이_비면_실패한다() {
		assertInvalid(() -> EventDetails.of(" ", EVENT_AT, "장소", 40, 20, DEADLINE, null, null), "행사명");
		assertInvalid(() -> EventDetails.of("행사", null, "장소", 40, 20, DEADLINE, null, null), "행사 일시");
		assertInvalid(() -> EventDetails.of("행사", EVENT_AT, null, 40, 20, DEADLINE, null, null), "장소");
		assertInvalid(() -> EventDetails.of("행사", EVENT_AT, "장소", null, 20, DEADLINE, null, null), "정원");
		assertInvalid(() -> EventDetails.of("행사", EVENT_AT, "장소", 40, 20, null, null, null), "신청 마감");
	}

	@Test
	void 컬럼_길이를_넘으면_실패한다() {
		assertInvalid(() -> EventDetails.of("가".repeat(101), EVENT_AT, "장소", 40, 20, DEADLINE, null, null),
				"행사명");
	}

	private static void assertInvalid(Runnable action, String messagePart) {
		assertThatThrownBy(action::run)
				.isInstanceOf(ApiException.class)
				.hasMessageContaining(messagePart)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
