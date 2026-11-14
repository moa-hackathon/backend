package com.moa.backend.event.domain;

/** 홈 카드와 대시보드 배너에 쓰는 행사 진행 상태. */
public enum EventProgressStatus {

	/** 필수 인원 미달: 신청 인원이 최소 필수 인원보다 적다. */
	BELOW_MIN,
	/** 진행 가능: 최소 필수 인원은 넘었고 정원에 여유가 있다. */
	AVAILABLE,
	/** 정원 임박: 최소 필수 인원은 넘었고 정원의 80% 이상이 찼다. */
	NEAR_CAPACITY,
	/** 정원 마감: 신청 인원이 정원에 도달했다. */
	FULL
}
