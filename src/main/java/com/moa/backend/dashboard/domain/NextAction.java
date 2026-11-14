package com.moa.backend.dashboard.domain;

import com.moa.backend.event.domain.EventProgress;

/**
 * 대시보드의 다음 할 일(F11). 진행 상태에 맞는 공지를 제안한다.
 *
 * @param type 제안 유형
 * @param message 화면에 그대로 보여줄 문구
 */
public record NextAction(Type type, String message) {

	public enum Type {
		/** 필수 인원 미달: 추가 모집 공지 */
		RECRUIT_MORE,
		/** 진행 가능: 신청 독려 공지 */
		ENCOURAGE,
		/** 정원 임박: 마감 임박 공지 */
		CLOSING_SOON,
		/** 정원 마감: 마감 안내 공지 */
		ANNOUNCE_CLOSED
	}

	public static NextAction from(EventProgress progress) {
		return switch (progress.status()) {
			case BELOW_MIN -> new NextAction(Type.RECRUIT_MORE,
					"최소 인원까지 %d명 남았어요. 추가 모집 공지를 만들어 드릴까요?".formatted(progress.remainingToMin()));
			case AVAILABLE -> new NextAction(Type.ENCOURAGE,
					"최소 인원은 넘었고 정원까지 %d명 남았어요. 신청 독려 공지를 만들어 드릴까요?"
							.formatted(progress.remainingToCapacity()));
			case NEAR_CAPACITY -> new NextAction(Type.CLOSING_SOON,
					"정원까지 %d명 남았어요. 마감 임박 공지를 만들어 드릴까요?".formatted(progress.remainingToCapacity()));
			case FULL -> new NextAction(Type.ANNOUNCE_CLOSED,
					"정원이 모두 찼어요. 신청 마감 안내 공지를 만들어 드릴까요?");
		};
	}
}
