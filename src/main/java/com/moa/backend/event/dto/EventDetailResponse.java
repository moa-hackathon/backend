package com.moa.backend.event.dto;

import com.moa.backend.event.entity.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "행사 단건 정보")
public record EventDetailResponse(
		Long id,
		String title,
		String description,
		LocalDateTime eventAt,
		String location,
		int capacity,
		int minCapacity,
		LocalDateTime applyDeadline,
		@Schema(description = "신청받을 때 추가로 물어볼 질문")
		List<String> extraQuestions,
		@Schema(description = "연결된 구글폼 링크. 없으면 null")
		String formUrl,
		@Schema(description = "공지 본문이 저장되어 있는지")
		boolean noticeReady,
		@Schema(description = "마지막 공지 발송 시각. 보내지 않았으면 null")
		LocalDateTime noticeSentAt,
		@Schema(description = "마지막 CSV 업로드 시각. 업로드 전이면 null")
		LocalDateTime lastSyncedAt
) {
	public static EventDetailResponse from(Event event) {
		List<String> extraQuestions = event.getExtraQuestions() == null ? List.of() : List.copyOf(event.getExtraQuestions());
		String noticeText = event.getNoticeText();
		return new EventDetailResponse(
				event.getId(),
				event.getTitle(),
				event.getDescription(),
				event.getEventAt(),
				event.getLocation(),
				event.getCapacity(),
				event.getMinCapacity(),
				event.getApplyDeadline(),
				extraQuestions,
				event.getFormUrl(),
				noticeText != null && !noticeText.isBlank(),
				event.getNoticeSentAt(),
				event.getLastSyncedAt());
	}
}
