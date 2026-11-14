package com.moa.backend.event.dto;

import com.moa.backend.event.domain.EventProgress;
import com.moa.backend.event.domain.EventProgressStatus;
import com.moa.backend.event.entity.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "홈 화면 행사 카드")
public record EventCardResponse(
		@Schema(description = "행사 id", example = "1")
		Long id,
		@Schema(description = "행사 이름", example = "모아 네트워킹 행사")
		String title,
		@Schema(description = "행사 일시", example = "2026-11-21T19:00:00")
		LocalDateTime eventAt,
		@Schema(description = "장소", example = "공학관 302호")
		String location,
		@Schema(description = "현재 신청 인원 (취소 제외)", example = "34")
		int applicantCount,
		@Schema(description = "정원", example = "40")
		int capacity,
		@Schema(description = "최소 필수 인원", example = "20")
		int minCapacity,
		@Schema(description = "신청 마감 일시", example = "2026-11-19T23:59:00")
		LocalDateTime applyDeadline,
		@Schema(description = "진행 상태", example = "NEAR_CAPACITY")
		EventProgressStatus status,
		@Schema(description = "최소 필수 인원까지 남은 인원, 달성했으면 0", example = "0")
		int remainingToMin,
		@Schema(description = "정원까지 남은 인원, 마감이면 0", example = "6")
		int remainingToCapacity,
		@Schema(description = "정원 대비 신청 비율(%), 0~100", example = "85")
		int fillPercent,
		@Schema(description = "신청 마감일까지 남은 일수(D-n의 n), 지났으면 음수", example = "5")
		long daysUntilDeadline,
		@Schema(description = "신청 마감 시각이 지났는지", example = "false")
		boolean deadlinePassed,
		@Schema(description = "카드 하단 안내 문구", example = "정원까지 6명 남음")
		String hint
) {

	public static EventCardResponse of(Event event, EventProgress progress) {
		return new EventCardResponse(
				event.getId(),
				event.getTitle(),
				event.getEventAt(),
				event.getLocation(),
				progress.applicantCount(),
				progress.capacity(),
				progress.minCapacity(),
				event.getApplyDeadline(),
				progress.status(),
				progress.remainingToMin(),
				progress.remainingToCapacity(),
				progress.fillPercent(),
				progress.daysUntilDeadline(),
				progress.deadlinePassed(),
				hintOf(progress));
	}

	/** 디자인의 카드 하단 문구. 진행 상태 하나로만 정한다. */
	static String hintOf(EventProgress progress) {
		return switch (progress.status()) {
			case BELOW_MIN -> "필수 %d명까지 %d명 남음".formatted(progress.minCapacity(), progress.remainingToMin());
			case AVAILABLE -> "필수 인원 달성";
			case NEAR_CAPACITY -> "정원까지 %d명 남음".formatted(progress.remainingToCapacity());
			case FULL -> "구글폼 응답 닫기 필요";
		};
	}
}
