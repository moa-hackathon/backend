package com.moa.backend.notice.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 행사 요약의 필수 항목. JSON으로는 {@link NoticeChatSummary}의 필드 이름으로 나간다. */
@Getter
@RequiredArgsConstructor
public enum NoticeChatField {
	TITLE("title", "행사명"),
	EVENT_AT("eventAt", "일시"),
	LOCATION("location", "장소"),
	CAPACITY("capacity", "정원"),
	MIN_CAPACITY("minCapacity", "최소 필수 인원"),
	APPLY_DEADLINE("applyDeadline", "신청 마감일");

	@JsonValue
	private final String key;
	private final String label;
}
