package com.moa.backend.dashboard.service;

import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationStatus;
import com.moa.backend.application.repository.ApplicationRepository;
import com.moa.backend.application.service.ApplicationStatusAllocator;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.dashboard.domain.ApplicationReview;
import com.moa.backend.dashboard.domain.ReviewedApplication;
import com.moa.backend.dashboard.dto.ApplicationCancelResponse;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 "확인 필요 항목"(F9)의 쓰기 동작. 신청을 지우지 않고 {@code CANCELED}로 바꾼다.
 *
 * <p>행을 지우면 CSV를 다시 올릴 때 같은 응답이 새 신청으로 되살아난다. 취소한 행은 row_hash가 남아 다시 들어오지 않는다.
 * 취소 뒤에는 확정/대기를 다시 매겨 대기자가 빈자리로 올라오게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationCheckService {

	private final EventRepository eventRepository;
	private final ApplicationRepository applicationRepository;

	/** "정리하기": 같은 학번의 신청 중 가장 빠른 것만 남기고 나머지를 취소한다. */
	public ApplicationCancelResponse resolveDuplicates(Club club, Long eventId) {
		Event event = findOwnedEvent(club, eventId);
		List<Application> applications = applicationRepository.findAllByEventIdOrderByAppliedAtAscIdAsc(eventId);

		List<Application> duplicates = ApplicationReview.of(applications).rows().stream()
				.filter(ReviewedApplication::duplicate)
				.map(ReviewedApplication::application)
				.toList();
		duplicates.forEach(application -> application.changeStatus(ApplicationStatus.CANCELED));

		return reallocate(event, applications, duplicates.size());
	}

	/** 신청 한 건을 취소한다. 이미 취소된 신청이면 아무것도 바꾸지 않는다. */
	public ApplicationCancelResponse cancel(Club club, Long eventId, Long applicationId) {
		Event event = findOwnedEvent(club, eventId);
		List<Application> applications = applicationRepository.findAllByEventIdOrderByAppliedAtAscIdAsc(eventId);

		Application target = applications.stream()
				.filter(application -> application.getId().equals(applicationId))
				.findFirst()
				.orElseThrow(() -> new ApiException(ErrorCode.APPLICATION_NOT_FOUND));
		if (target.getStatus() == ApplicationStatus.CANCELED) {
			return reallocate(event, applications, 0);
		}
		target.changeStatus(ApplicationStatus.CANCELED);

		return reallocate(event, applications, 1);
	}

	private ApplicationCancelResponse reallocate(Event event, List<Application> applications, int canceledCount) {
		ApplicationStatusAllocator.Result allocation =
				ApplicationStatusAllocator.allocate(applications, event.getCapacity());
		return new ApplicationCancelResponse(canceledCount, allocation.confirmedCount(), allocation.waitingCount());
	}

	private Event findOwnedEvent(Club club, Long eventId) {
		return eventRepository.findByIdAndClubId(eventId, club.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND));
	}
}
