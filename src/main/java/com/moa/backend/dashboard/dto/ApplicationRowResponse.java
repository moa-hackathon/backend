package com.moa.backend.dashboard.dto;

import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationStatus;
import com.moa.backend.dashboard.domain.DisplayStatus;
import com.moa.backend.dashboard.domain.RequiredField;
import com.moa.backend.dashboard.domain.ReviewedApplication;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 신청자 표의 한 행.
 *
 * @param status 저장된 상태 (CONFIRMED / WAITING / CANCELED)
 * @param displayStatus 표에 붙일 배지 (CANCELED > DUPLICATE > MISSING > CONFIRMED / WAITING)
 * @param displayStatusLabel 배지의 한글명 (취소, 중복, 누락, 확정, 대기)
 * @param duplicate 중복 여부 (배지가 다른 것이어도 true일 수 있음)
 * @param missingFields 비어 있는 필수 항목
 */
public record ApplicationRowResponse(
		Long id,
		String name,
		String studentId,
		String contact,
		Map<String, String> answers,
		LocalDateTime appliedAt,
		ApplicationStatus status,
		DisplayStatus displayStatus,
		String displayStatusLabel,
		boolean duplicate,
		List<RequiredField> missingFields
) {

	public static ApplicationRowResponse from(ReviewedApplication reviewed) {
		Application application = reviewed.application();
		return new ApplicationRowResponse(
				application.getId(),
				application.getName(),
				application.getStudentId(),
				application.getContact(),
				application.getAnswers() == null ? Map.of() : application.getAnswers(),
				application.getAppliedAt(),
				application.getStatus(),
				reviewed.displayStatus(),
				reviewed.displayStatus().getLabel(),
				reviewed.duplicate(),
				reviewed.missingFields());
	}
}
