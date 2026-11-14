package com.moa.backend.dashboard.dto;

import com.moa.backend.event.entity.Event;
import java.time.LocalDateTime;

/**
 * @param lastSyncedAt 마지막 CSV 업로드 시각("마지막 업데이트"), 업로드 전이면 null
 */
public record EventSummaryResponse(
		Long id,
		String title,
		LocalDateTime eventAt,
		String location,
		LocalDateTime lastSyncedAt
) {

	public static EventSummaryResponse from(Event event) {
		return new EventSummaryResponse(event.getId(), event.getTitle(), event.getEventAt(), event.getLocation(),
				event.getLastSyncedAt());
	}
}
