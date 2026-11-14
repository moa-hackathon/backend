package com.moa.backend.dashboard.domain;

import java.util.function.Predicate;

/**
 * 신청자 목록 필터. 확정/대기는 표시 배지 기준이라 중복·누락·취소 배지가 붙은 행은 빠진다. 그런 행은 확인 필요에서 본다.
 */
public enum ApplicationFilter {

	ALL(row -> true),
	CONFIRMED(row -> row.displayStatus() == DisplayStatus.CONFIRMED),
	WAITING(row -> row.displayStatus() == DisplayStatus.WAITING),
	NEEDS_CHECK(ReviewedApplication::needsCheck);

	private final Predicate<ReviewedApplication> predicate;

	ApplicationFilter(Predicate<ReviewedApplication> predicate) {
		this.predicate = predicate;
	}

	public boolean matches(ReviewedApplication row) {
		return predicate.test(row);
	}
}
