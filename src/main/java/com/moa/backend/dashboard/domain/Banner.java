package com.moa.backend.dashboard.domain;

import com.moa.backend.event.domain.EventProgress;

/**
 * 대시보드 상단 배너(F6). 진행 상태를 한 줄로 알려 준다.
 *
 * @param title 굵게 보여줄 상태명
 * @param message 상태 설명 문구
 */
public record Banner(String title, String message) {

	public static Banner from(EventProgress progress) {
		return switch (progress.status()) {
			case BELOW_MIN -> new Banner("필수 인원 미달",
					"최소 인원까지 %d명 남았어요.".formatted(progress.remainingToMin()));
			case AVAILABLE -> new Banner("진행 가능",
					"최소 인원은 넘었고, 정원까지 %d명 남았어요.".formatted(progress.remainingToCapacity()));
			case NEAR_CAPACITY -> new Banner("진행 가능 · 정원 임박",
					"최소 인원은 넘었고, 정원까지 %d명 남았어요.".formatted(progress.remainingToCapacity()));
			case FULL -> new Banner("정원 마감", "정원이 모두 찼어요.");
		};
	}
}
