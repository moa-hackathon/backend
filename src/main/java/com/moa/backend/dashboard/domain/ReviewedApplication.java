package com.moa.backend.dashboard.domain;

import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationStatus;
import java.util.List;

/**
 * 중복·누락 판정을 마친 신청 한 건.
 *
 * @param duplicate 같은 학번의 더 이른 신청이 있어 중복으로 본 행인지 (취소 행은 항상 false)
 * @param missingFields 비어 있는 필수 항목 (취소 행은 항상 빈 목록)
 * @param displayStatus 표에 표시할 배지
 */
public record ReviewedApplication(
		Application application,
		boolean duplicate,
		List<RequiredField> missingFields,
		DisplayStatus displayStatus
) {

	public static ReviewedApplication of(Application application, boolean duplicate, List<RequiredField> missingFields) {
		return new ReviewedApplication(application, duplicate, List.copyOf(missingFields),
				decide(application.getStatus(), duplicate, !missingFields.isEmpty()));
	}

	private static DisplayStatus decide(ApplicationStatus status, boolean duplicate, boolean missing) {
		if (status == ApplicationStatus.CANCELED) {
			return DisplayStatus.CANCELED;
		}
		if (duplicate) {
			return DisplayStatus.DUPLICATE;
		}
		if (missing) {
			return DisplayStatus.MISSING;
		}
		return status == ApplicationStatus.CONFIRMED ? DisplayStatus.CONFIRMED : DisplayStatus.WAITING;
	}

	public boolean canceled() {
		return application.getStatus() == ApplicationStatus.CANCELED;
	}

	public boolean missing() {
		return !missingFields.isEmpty();
	}

	/** 운영진 확인이 필요한 행: 중복 또는 누락. */
	public boolean needsCheck() {
		return duplicate || missing();
	}
}
