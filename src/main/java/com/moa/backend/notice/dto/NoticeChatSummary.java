package com.moa.backend.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 행사 요약 (F1). 필드 이름과 타입은 {@code Event} 엔티티와 같다. 아직 모르는 값은 null이다.
 */
@Schema(description = "행사 요약 (F1). 아직 모르는 값은 null")
public record NoticeChatSummary(
		@Schema(description = "행사명", example = "모아 네트워킹 행사")
		String title,

		@Schema(description = "행사 일시 (Asia/Seoul)", example = "2026-11-20T19:00:00")
		LocalDateTime eventAt,

		@Schema(description = "장소", example = "공학관 302호")
		String location,

		@Schema(description = "정원", example = "40")
		Integer capacity,

		@Schema(description = "최소 필수 인원", example = "20")
		Integer minCapacity,

		@Schema(description = "신청 마감일 (날짜만 말하면 그날 23:59)", example = "2026-11-18T23:59:00")
		LocalDateTime applyDeadline,

		@Schema(description = "신청받을 때 추가로 물어볼 질문", example = "[\"뒤풀이 참석 여부\"]")
		List<String> extraQuestions,

		@Schema(description = "기타 안내 (음식, 준비물 등). 공지 초안에 쓴다", example = "피자가 준비되어 있어요.")
		String description) {

	public static final int MAX_TITLE_LENGTH = 100;
	public static final int MAX_LOCATION_LENGTH = 255;
	public static final int MAX_EXTRA_QUESTIONS = 10;
	public static final int MAX_EXTRA_QUESTION_LENGTH = 100;
	public static final int MAX_DESCRIPTION_LENGTH = 1000;

	public NoticeChatSummary {
		extraQuestions = extraQuestions == null ? List.of() : List.copyOf(extraQuestions);
	}

	public static NoticeChatSummary empty() {
		return new NoticeChatSummary(null, null, null, null, null, null, List.of(), null);
	}

	/** 아직 채워지지 않은 필수 항목. */
	public List<NoticeChatField> missingFields() {
		List<NoticeChatField> missing = new ArrayList<>();
		if (title == null) {
			missing.add(NoticeChatField.TITLE);
		}
		if (eventAt == null) {
			missing.add(NoticeChatField.EVENT_AT);
		}
		if (location == null) {
			missing.add(NoticeChatField.LOCATION);
		}
		if (capacity == null) {
			missing.add(NoticeChatField.CAPACITY);
		}
		if (minCapacity == null) {
			missing.add(NoticeChatField.MIN_CAPACITY);
		}
		if (applyDeadline == null) {
			missing.add(NoticeChatField.APPLY_DEADLINE);
		}
		return missing;
	}
}
