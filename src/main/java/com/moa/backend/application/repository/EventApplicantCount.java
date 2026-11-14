package com.moa.backend.application.repository;

/**
 * 행사별 신청 인원 집계 결과.
 *
 * @param eventId 행사 id
 * @param count 신청 인원
 */
public record EventApplicantCount(Long eventId, long count) {
}
