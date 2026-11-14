package com.moa.backend.dashboard.dto;

/**
 * 신청 취소(중복 정리 포함) 결과.
 *
 * @param canceledCount 이번 요청으로 취소된 신청 수
 * @param confirmedCount 재계산 후 확정 인원
 * @param waitingCount 재계산 후 대기 인원
 */
public record ApplicationCancelResponse(
		int canceledCount,
		int confirmedCount,
		int waitingCount
) {
}
