package com.moa.backend.dashboard.service;

import com.moa.backend.application.repository.ApplicationRepository;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.dashboard.domain.AnswerAggregator;
import com.moa.backend.dashboard.domain.ApplicationFilter;
import com.moa.backend.dashboard.domain.ApplicationReview;
import com.moa.backend.dashboard.domain.NextAction;
import com.moa.backend.dashboard.dto.ApplicationListResponse;
import com.moa.backend.dashboard.dto.ApplicationRowResponse;
import com.moa.backend.dashboard.dto.CheckSummaryResponse;
import com.moa.backend.dashboard.dto.DashboardResponse;
import com.moa.backend.dashboard.dto.EventSummaryResponse;
import com.moa.backend.dashboard.export.ApplicationExcelWriter;
import com.moa.backend.dashboard.export.ExcelDownload;
import com.moa.backend.event.domain.EventProgress;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

	private final EventRepository eventRepository;
	private final ApplicationRepository applicationRepository;

	public DashboardResponse getDashboard(Club club, Long eventId, LocalDateTime now) {
		Event event = findOwnedEvent(club, eventId);
		ApplicationReview review = review(event);
		EventProgress progress = EventProgress.of(review.applicantCount(), event.getCapacity(),
				event.getMinCapacity(), event.getApplyDeadline(), now);
		return new DashboardResponse(
				EventSummaryResponse.from(event),
				progress,
				NextAction.from(progress),
				AnswerAggregator.aggregate(review),
				CheckSummaryResponse.from(review));
	}

	public ApplicationListResponse getApplications(Club club, Long eventId, ApplicationFilter filter) {
		Event event = findOwnedEvent(club, eventId);
		List<ApplicationRowResponse> rows = review(event).filter(filter).stream()
				.map(ApplicationRowResponse::from)
				.toList();
		return new ApplicationListResponse(filter, rows.size(), rows);
	}

	public ExcelDownload exportApplications(Club club, Long eventId) {
		Event event = findOwnedEvent(club, eventId);
		byte[] content = ApplicationExcelWriter.write(review(event));
		return ExcelDownload.forEvent(event.getId(), event.getTitle(), content);
	}

	private Event findOwnedEvent(Club club, Long eventId) {
		return eventRepository.findByIdAndClubId(eventId, club.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND));
	}

	private ApplicationReview review(Event event) {
		return ApplicationReview.of(applicationRepository.findAllByEventIdOrderByAppliedAtAscIdAsc(event.getId()));
	}
}
