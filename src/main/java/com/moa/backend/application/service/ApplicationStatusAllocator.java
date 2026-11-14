package com.moa.backend.application.service;

import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationStatus;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 행사 신청의 확정/대기를 다시 매긴다. 스프링·DB와 무관한 순수 로직이다.
 *
 * <p>취소가 아닌 신청을 받은 순서대로 보며 정원까지 {@code CONFIRMED}, 넘으면 {@code WAITING}으로 둔다.
 * 같은 학번(공백 제외, 비어 있지 않은 값)이 앞에 이미 있는 중복 신청은 자리를 차지하지 않고 {@code WAITING}이다.
 * {@code CANCELED}는 건드리지 않고 중복 판별에도 쓰지 않는다.
 */
public final class ApplicationStatusAllocator {

	private ApplicationStatusAllocator() {
	}

	/**
	 * @param applications 신청 시각, id 오름차순으로 정렬된 행사의 모든 신청
	 * @param capacity 정원
	 */
	public static Result allocate(List<Application> applications, int capacity) {
		Set<String> seenStudentIds = new HashSet<>();
		int confirmed = 0;
		int waiting = 0;
		for (Application application : applications) {
			if (application.getStatus() == ApplicationStatus.CANCELED) {
				continue;
			}
			String studentKey = studentKey(application.getStudentId());
			boolean duplicate = studentKey != null && !seenStudentIds.add(studentKey);
			if (!duplicate && confirmed < capacity) {
				application.changeStatus(ApplicationStatus.CONFIRMED);
				confirmed++;
			} else {
				application.changeStatus(ApplicationStatus.WAITING);
				waiting++;
			}
		}
		return new Result(confirmed, waiting);
	}

	private static String studentKey(String studentId) {
		if (studentId == null) {
			return null;
		}
		String key = studentId.replaceAll("\\s+", "");
		return key.isEmpty() ? null : key;
	}

	public record Result(int confirmedCount, int waitingCount) {
	}
}
