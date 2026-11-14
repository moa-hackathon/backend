package com.moa.backend.dashboard.service;

import static com.moa.backend.application.entity.ApplicationStatus.CANCELED;
import static com.moa.backend.application.entity.ApplicationStatus.CONFIRMED;
import static com.moa.backend.application.entity.ApplicationStatus.WAITING;
import static com.moa.backend.dashboard.domain.ApplicationFixture.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.moa.backend.application.entity.Application;
import com.moa.backend.application.repository.ApplicationRepository;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.dashboard.dto.ApplicationCancelResponse;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApplicationCheckServiceTest {

	private static final long CLUB_ID = 1L;
	private static final long EVENT_ID = 10L;

	@Mock
	private EventRepository eventRepository;
	@Mock
	private ApplicationRepository applicationRepository;
	@InjectMocks
	private ApplicationCheckService applicationCheckService;

	private Club club;
	private List<Application> applications;

	@BeforeEach
	void setUp() {
		club = Club.builder().name("모아").inviteCode("code").build();
		ReflectionTestUtils.setField(club, "id", CLUB_ID);
		Event event = Event.builder()
				.club(club)
				.title("모아 네트워킹 행사")
				.location("공학관 302호")
				.eventAt(LocalDateTime.of(2026, 11, 21, 19, 0))
				.capacity(2)
				.minCapacity(1)
				.applyDeadline(LocalDateTime.of(2026, 11, 19, 23, 59))
				.shareCode("share")
				.build();
		ReflectionTestUtils.setField(event, "id", EVENT_ID);

		// 정원 2: 1번·2번 확정, 3번은 1번과 같은 학번(중복), 4번 대기
		applications = List.of(
				application(1, "김모아", "20230015", "010-1", CONFIRMED, 0),
				application(2, "이모아", "20240001", "010-2", CONFIRMED, 10),
				application(3, "김모아", "2023 0015", "010-1", WAITING, 20),
				application(4, "박모아", "20250007", "010-4", WAITING, 30));
		given(eventRepository.findByIdAndClubId(EVENT_ID, CLUB_ID)).willReturn(Optional.of(event));
	}

	private void givenApplications() {
		given(applicationRepository.findAllByEventIdOrderByAppliedAtAscIdAsc(EVENT_ID)).willReturn(applications);
	}

	@Test
	void 중복_정리는_가장_빠른_신청만_남기고_나머지를_취소한다() {
		givenApplications();

		ApplicationCancelResponse response = applicationCheckService.resolveDuplicates(club, EVENT_ID);

		assertThat(response).isEqualTo(new ApplicationCancelResponse(1, 2, 1));
		assertThat(applications).extracting(Application::getStatus)
				.containsExactly(CONFIRMED, CONFIRMED, CANCELED, WAITING);
	}

	@Test
	void 중복이_없으면_아무것도_취소하지_않는다() {
		applications.get(2).changeStatus(CANCELED);
		givenApplications();

		ApplicationCancelResponse response = applicationCheckService.resolveDuplicates(club, EVENT_ID);

		assertThat(response.canceledCount()).isZero();
		assertThat(applications).extracting(Application::getStatus)
				.containsExactly(CONFIRMED, CONFIRMED, CANCELED, WAITING);
	}

	@Test
	void 확정_신청을_취소하면_대기자가_확정으로_올라온다() {
		givenApplications();

		ApplicationCancelResponse response = applicationCheckService.cancel(club, EVENT_ID, 2L);

		assertThat(response).isEqualTo(new ApplicationCancelResponse(1, 2, 1));
		assertThat(applications).extracting(Application::getStatus)
				.containsExactly(CONFIRMED, CANCELED, WAITING, CONFIRMED);
	}

	@Test
	void 중복의_앞쪽_신청을_취소하면_뒤쪽_신청이_자리를_받는다() {
		givenApplications();

		applicationCheckService.cancel(club, EVENT_ID, 1L);

		assertThat(applications).extracting(Application::getStatus)
				.containsExactly(CANCELED, CONFIRMED, CONFIRMED, WAITING);
	}

	@Test
	void 이미_취소된_신청을_다시_취소해도_바뀌지_않는다() {
		applications.get(3).changeStatus(CANCELED);
		givenApplications();

		ApplicationCancelResponse response = applicationCheckService.cancel(club, EVENT_ID, 4L);

		assertThat(response.canceledCount()).isZero();
		assertThat(applications.get(3).getStatus()).isEqualTo(CANCELED);
	}

	@Test
	void 행사에_없는_신청이면_신청_없음으로_응답한다() {
		givenApplications();

		assertThatThrownBy(() -> applicationCheckService.cancel(club, EVENT_ID, 999L))
				.isInstanceOf(ApiException.class)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
	}

	@Test
	void 다른_동아리의_행사면_행사_없음으로_응답한다() {
		given(eventRepository.findByIdAndClubId(EVENT_ID, CLUB_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> applicationCheckService.resolveDuplicates(club, EVENT_ID))
				.isInstanceOf(ApiException.class)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.EVENT_NOT_FOUND);
	}
}
