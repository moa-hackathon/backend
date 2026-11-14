package com.moa.backend.dashboard.domain;

import com.moa.backend.application.entity.ApplicationStatus;
import java.util.function.Predicate;

/**
 * 신청자 목록 필터. 확정/대기는 저장된 상태 기준이라 누락·중복 배지가 붙은 행도 포함된다.
 */
public enum ApplicationFilter {

	ALL(row -> true),
	CONFIRMED(row -> row.application().getStatus() == ApplicationStatus.CONFIRMED),
	WAITING(row -> row.application().getStatus() == ApplicationStatus.WAITING),
	NEEDS_CHECK(ReviewedApplication::needsCheck);

	private final Predicate<ReviewedApplication> predicate;

	ApplicationFilter(Predicate<ReviewedApplication> predicate) {
		this.predicate = predicate;
	}

	public boolean matches(ReviewedApplication row) {
		return predicate.test(row);
	}
}
