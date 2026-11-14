package com.moa.backend.event.service;

import com.moa.backend.application.entity.ApplicationStatus;
import com.moa.backend.application.repository.ApplicationRepository;
import com.moa.backend.application.repository.EventApplicantCount;
import com.moa.backend.club.entity.Club;
import com.moa.backend.event.domain.EventProgress;
import com.moa.backend.event.dto.EventCardResponse;
import com.moa.backend.event.dto.EventListResponse;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryService {

	private final EventRepository eventRepository;
	private final ApplicationRepository applicationRepository;

	/** 동아리의 진행 중인 행사(행사 당일 자정까지 포함)를 행사 일시 오름차순 카드 목록으로 돌려준다. */
	public EventListResponse getOngoingEvents(Club club, LocalDateTime now) {
		List<Event> events = eventRepository.findAllByClubIdAndEventAtGreaterThanEqualOrderByEventAtAscIdAsc(club.getId(), now.toLocalDate().atStartOfDay());
		Map<Long, Long> applicantCounts = countApplicants(events);

		List<EventCardResponse> cards = events.stream()
				.map(event -> {
					int applicantCount = Math.toIntExact(applicantCounts.getOrDefault(event.getId(), 0L));
					EventProgress progress = EventProgress.of(applicantCount, event.getCapacity(),
							event.getMinCapacity(), event.getApplyDeadline(), now);
					return EventCardResponse.of(event, progress);
				})
				.toList();
		return EventListResponse.of(cards);
	}

	private Map<Long, Long> countApplicants(List<Event> events) {
		if (events.isEmpty()) {
			return Map.of();
		}
		List<Long> eventIds = events.stream().map(Event::getId).toList();
		return applicationRepository.countByEventIdsExcludingStatus(eventIds, ApplicationStatus.CANCELED).stream()
				.collect(Collectors.toMap(EventApplicantCount::eventId, EventApplicantCount::count));
	}
}
