package com.moa.backend.event.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 행사의 신청 진행 상황. 홈(행사 카드)과 대시보드(배너, 요약 카드)가 같은 기준을 쓰도록 한곳에서 계산한다.
 *
 * @param applicantCount 현재 신청 인원 (취소 제외)
 * @param capacity 정원
 * @param minCapacity 최소 필수 인원
 * @param status 진행 상태
 * @param remainingToMin 최소 필수 인원까지 남은 인원, 달성했으면 0
 * @param remainingToCapacity 정원까지 남은 인원, 마감이면 0
 * @param fillPercent 정원 대비 신청 비율(%), 0~100
 * @param daysUntilDeadline 신청 마감일까지 남은 일수(D-n의 n), 마감일이 지났으면 음수
 * @param deadlinePassed 신청 마감 시각이 지났는지
 */
public record EventProgress(
		int applicantCount,
		int capacity,
		int minCapacity,
		EventProgressStatus status,
		int remainingToMin,
		int remainingToCapacity,
		int fillPercent,
		long daysUntilDeadline,
		boolean deadlinePassed
) {

	/** 정원의 이 비율(%) 이상이 차면 정원 임박으로 본다. */
	public static final int NEAR_CAPACITY_PERCENT = 80;

	public static EventProgress of(int applicantCount, int capacity, int minCapacity,
			LocalDateTime applyDeadline, LocalDateTime now) {
		int remainingToMin = Math.max(minCapacity - applicantCount, 0);
		int remainingToCapacity = Math.max(capacity - applicantCount, 0);
		int fillPercent = capacity <= 0 ? 100 : Math.min(Math.round(applicantCount * 100f / capacity), 100);

		EventProgressStatus status;
		if (applicantCount >= capacity) {
			status = EventProgressStatus.FULL;
		} else if (applicantCount < minCapacity) {
			status = EventProgressStatus.BELOW_MIN;
		} else if (applicantCount * 100 >= capacity * NEAR_CAPACITY_PERCENT) {
			status = EventProgressStatus.NEAR_CAPACITY;
		} else {
			status = EventProgressStatus.AVAILABLE;
		}

		long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.from(now), LocalDate.from(applyDeadline));
		return new EventProgress(applicantCount, capacity, minCapacity, status, remainingToMin,
				remainingToCapacity, fillPercent, daysUntilDeadline, now.isAfter(applyDeadline));
	}
}
