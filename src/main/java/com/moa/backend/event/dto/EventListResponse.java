package com.moa.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "홈 화면 진행 중인 행사 목록")
public record EventListResponse(
		@Schema(description = "진행 중인 행사 수", example = "4")
		int count,
		@Schema(description = "진행 중인 행사 카드, 행사 일시 오름차순")
		List<EventCardResponse> events
) {

	public static EventListResponse of(List<EventCardResponse> events) {
		return new EventListResponse(events.size(), events);
	}
}
