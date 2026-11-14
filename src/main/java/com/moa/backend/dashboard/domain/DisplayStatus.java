package com.moa.backend.dashboard.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 신청자 표에 붙는 배지. 한 행에는 하나만 붙고, 우선순위는 선언 순서(취소 > 중복 > 누락 > 확정/대기)다.
 */
@Getter
@RequiredArgsConstructor
public enum DisplayStatus {

	CANCELED("취소"),
	DUPLICATE("중복"),
	MISSING("누락"),
	CONFIRMED("확정"),
	WAITING("대기");

	private final String label;
}
