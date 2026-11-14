package com.moa.backend.event.dto;

import com.moa.backend.event.entity.Event;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 만든 행사.
 *
 * @param id 행사 ID. 다음 화면(구글폼 링크 연결, 발송)에서 쓴다
 * @param shareCode 부원용 공유 링크 코드
 */
public record EventCreateResponse(
		Long id,
		String shareCode,
		String title,
		LocalDateTime eventAt,
		String location,
		int capacity,
		int minCapacity,
		LocalDateTime applyDeadline,
		List<String> extraQuestions,
		String description,
		String noticeText
) {

	public static EventCreateResponse from(Event event) {
		return new EventCreateResponse(event.getId(), event.getShareCode(), event.getTitle(), event.getEventAt(),
				event.getLocation(), event.getCapacity(), event.getMinCapacity(), event.getApplyDeadline(),
				event.getExtraQuestions() == null ? List.of() : event.getExtraQuestions(), event.getDescription(),
				event.getNoticeText());
	}
}
