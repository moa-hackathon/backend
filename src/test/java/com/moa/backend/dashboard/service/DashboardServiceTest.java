package com.moa.backend.dashboard.service;

import static com.moa.backend.application.entity.ApplicationStatus.CANCELED;
import static com.moa.backend.application.entity.ApplicationStatus.CONFIRMED;
import static com.moa.backend.application.entity.ApplicationStatus.WAITING;
import static com.moa.backend.dashboard.domain.ApplicationFixture.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.moa.backend.application.repository.ApplicationRepository;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.dashboard.domain.ApplicationFilter;
import com.moa.backend.dashboard.domain.DisplayStatus;
import com.moa.backend.dashboard.domain.NextAction;
import com.moa.backend.dashboard.domain.RequiredField;
import com.moa.backend.dashboard.dto.ApplicationListResponse;
import com.moa.backend.dashboard.dto.ApplicationRowResponse;
import com.moa.backend.dashboard.dto.DashboardResponse;
import com.moa.backend.dashboard.export.ExcelDownload;
import com.moa.backend.event.domain.EventProgressStatus;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 12, 40);
	private static final long CLUB_ID = 1L;
	private static final long EVENT_ID = 10L;

	@Mock
	private EventRepository eventRepository;
	@Mock
	private ApplicationRepository applicationRepository;
	@InjectMocks
	private DashboardService dashboardService;

	private Club club;
	private Event event;

	@BeforeEach
	void setUp() {
		club = Club.builder().name("모아").inviteCode("code").build();
		ReflectionTestUtils.setField(club, "id", CLUB_ID);
		event = Event.builder()
				.club(club)
				.title("모아 네트워킹 행사")
				.location("공학관 302호")
				.eventAt(LocalDateTime.of(2026, 11, 21, 19, 0))
				.capacity(5)
				.minCapacity(2)
				.applyDeadline(LocalDateTime.of(2026, 11, 19, 23, 59))
				.shareCode("share")
				.lastSyncedAt(NOW)
				.build();
		ReflectionTestUtils.setField(event, "id", EVENT_ID);
	}

	private void givenApplications() {
		given(eventRepository.findByIdAndClubId(EVENT_ID, CLUB_ID)).willReturn(Optional.of(event));
		given(applicationRepository.findAllByEventIdOrderByAppliedAtAscIdAsc(EVENT_ID)).willReturn(List.of(
				application(1, "김모아", "20230015", "010-1", CONFIRMED, 0, Map.of("뒤풀이", "참석")),
				application(2, "이모아", "20240001", null, CONFIRMED, 10, Map.of("뒤풀이", "불참")),
				application(3, "박모아", "20250007", "010-3", CONFIRMED, 20, Map.of("뒤풀이", "참석")),
				application(4, "김모아", "20230015", "010-1", WAITING, 30, Map.of("뒤풀이", "참석")),
				application(5, "최모아", "20220031", "010-5", CANCELED, 40, Map.of("뒤풀이", "불참"))));
	}

	@Test
	void 대시보드_요약을_만든다() {
		givenApplications();

		DashboardResponse response = dashboardService.getDashboard(club, EVENT_ID, NOW);

		assertThat(response.event().id()).isEqualTo(EVENT_ID);
		assertThat(response.event().title()).isEqualTo("모아 네트워킹 행사");
		assertThat(response.event().lastSyncedAt()).isEqualTo(NOW);
		assertThat(response.progress().applicantCount()).isEqualTo(4);
		assertThat(response.progress().status()).isEqualTo(EventProgressStatus.NEAR_CAPACITY);
		assertThat(response.progress().remainingToCapacity()).isEqualTo(1);
		assertThat(response.progress().daysUntilDeadline()).isEqualTo(5);
		assertThat(response.nextAction().type()).isEqualTo(NextAction.Type.CLOSING_SOON);
		assertThat(response.nextAction().message()).isEqualTo("정원까지 1명 남았어요. 마감 임박 공지를 만들어 드릴까요?");
		assertThat(response.answerStats()).singleElement().satisfies(stat -> {
			assertThat(stat.question()).isEqualTo("뒤풀이");
			assertThat(stat.options()).extracting(option -> option.value() + ":" + option.count() + ":" + option.percent())
					.containsExactly("참석:3:75", "불참:1:25");
		});
		assertThat(response.checks().duplicateCount()).isEqualTo(1);
		assertThat(response.checks().missingCount()).isEqualTo(1);
		assertThat(response.checks().missingFields()).singleElement().satisfies(field -> {
			assertThat(field.field()).isEqualTo(RequiredField.CONTACT);
			assertThat(field.count()).isEqualTo(1);
		});
		assertThat(response.checks().missingSummary()).isEqualTo("연락처 없음");
	}

	@Test
	void 신청이_없으면_미달_상태와_빈_집계를_준다() {
		given(eventRepository.findByIdAndClubId(EVENT_ID, CLUB_ID)).willReturn(Optional.of(event));
		given(applicationRepository.findAllByEventIdOrderByAppliedAtAscIdAsc(EVENT_ID)).willReturn(List.of());

		DashboardResponse response = dashboardService.getDashboard(club, EVENT_ID, NOW);

		assertThat(response.progress().applicantCount()).isZero();
		assertThat(response.progress().status()).isEqualTo(EventProgressStatus.BELOW_MIN);
		assertThat(response.nextAction().type()).isEqualTo(NextAction.Type.RECRUIT_MORE);
		assertThat(response.answerStats()).isEmpty();
		assertThat(response.checks().duplicateCount()).isZero();
		assertThat(response.checks().missingFields()).isEmpty();
		assertThat(response.checks().missingSummary()).isNull();
	}

	@Test
	void 필터를_적용한_목록과_건수를_준다() {
		givenApplications();

		ApplicationListResponse all = dashboardService.getApplications(club, EVENT_ID, ApplicationFilter.ALL);
		ApplicationListResponse needsCheck = dashboardService.getApplications(club, EVENT_ID, ApplicationFilter.NEEDS_CHECK);

		assertThat(all.totalCount()).isEqualTo(5);
		assertThat(all.applications()).extracting(ApplicationRowResponse::displayStatus).containsExactly(
				DisplayStatus.CONFIRMED, DisplayStatus.MISSING, DisplayStatus.CONFIRMED, DisplayStatus.DUPLICATE,
				DisplayStatus.CANCELED);
		assertThat(all.applications().get(1).contact()).isNull();
		assertThat(all.applications().get(0).contact()).isEqualTo("010-1");
		assertThat(needsCheck.filter()).isEqualTo(ApplicationFilter.NEEDS_CHECK);
		assertThat(needsCheck.totalCount()).isEqualTo(2);
		assertThat(needsCheck.applications()).extracting(ApplicationRowResponse::id).containsExactly(2L, 4L);
	}

	@Test
	void 엑셀_파일명에_행사명을_넣는다() {
		givenApplications();

		ExcelDownload download = dashboardService.exportApplications(club, EVENT_ID);

		assertThat(download.fileName()).isEqualTo("모아 네트워킹 행사_신청자.xlsx");
		assertThat(download.content()).isNotEmpty();
	}

	@Test
	void 다른_동아리의_행사면_행사_없음으로_응답한다() {
		given(eventRepository.findByIdAndClubId(EVENT_ID, CLUB_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> dashboardService.getDashboard(club, EVENT_ID, NOW))
				.isInstanceOf(ApiException.class)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.EVENT_NOT_FOUND);
	}
}
