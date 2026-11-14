package com.moa.backend.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.moa.backend.application.entity.ApplicationStatus;
import com.moa.backend.application.repository.ApplicationRepository;
import com.moa.backend.application.repository.EventApplicantCount;
import com.moa.backend.club.entity.Club;
import com.moa.backend.event.domain.EventProgressStatus;
import com.moa.backend.event.dto.EventCardResponse;
import com.moa.backend.event.dto.EventListResponse;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 12, 40);

	@Mock
	private EventRepository eventRepository;

	@Mock
	private ApplicationRepository applicationRepository;

	@InjectMocks
	private EventQueryService eventQueryService;

	private Club club;

	@BeforeEach
	void setUp() {
		club = Club.builder().name("모아").inviteCode("code").build();
		ReflectionTestUtils.setField(club, "id", 1L);
	}

	@Test
	void 디자인의_네_카드를_그대로_만든다() {
		Event networking = event(10L, "모아 네트워킹 행사", LocalDateTime.of(2026, 11, 21, 19, 0), 40, 20,
				LocalDateTime.of(2026, 11, 19, 23, 59));
		Event study = event(11L, "알고리즘 스터디 발표회", LocalDateTime.of(2026, 11, 25, 18, 30), 30, 15,
				LocalDateTime.of(2026, 11, 17, 23, 59));
		Event welcome = event(12L, "신입 부원 환영회", LocalDateTime.of(2026, 11, 28, 18, 0), 60, 30,
				LocalDateTime.of(2026, 11, 24, 23, 59));
		Event hackathon = event(13L, "해커톤 OT", LocalDateTime.of(2026, 11, 20, 19, 0), 50, 20,
				LocalDateTime.of(2026, 11, 18, 23, 59));
		given(eventRepository.findAllByClubIdAndEventAtAfterOrderByEventAtAscIdAsc(1L, NOW))
				.willReturn(List.of(networking, study, welcome, hackathon));
		given(applicationRepository.countByEventIdsExcludingStatus(List.of(10L, 11L, 12L, 13L),
				ApplicationStatus.CANCELED))
				.willReturn(List.of(
						new EventApplicantCount(10L, 34),
						new EventApplicantCount(11L, 9),
						new EventApplicantCount(12L, 31),
						new EventApplicantCount(13L, 50)));

		EventListResponse response = eventQueryService.getOngoingEvents(club, NOW);

		assertThat(response.count()).isEqualTo(4);
		assertThat(response.events())
				.extracting(EventCardResponse::id, EventCardResponse::applicantCount, EventCardResponse::status,
						EventCardResponse::hint)
				.containsExactly(
						tuple(10L, 34, EventProgressStatus.NEAR_CAPACITY, "정원까지 6명 남음"),
						tuple(11L, 9, EventProgressStatus.BELOW_MIN, "필수 15명까지 6명 남음"),
						tuple(12L, 31, EventProgressStatus.AVAILABLE, "필수 인원 달성"),
						tuple(13L, 50, EventProgressStatus.FULL, "구글폼 응답 닫기 필요"));

		EventCardResponse first = response.events().get(0);
		assertThat(first.title()).isEqualTo("모아 네트워킹 행사");
		assertThat(first.location()).isEqualTo("공학관 302호");
		assertThat(first.eventAt()).isEqualTo(LocalDateTime.of(2026, 11, 21, 19, 0));
		assertThat(first.applyDeadline()).isEqualTo(LocalDateTime.of(2026, 11, 19, 23, 59));
		assertThat(first.capacity()).isEqualTo(40);
		assertThat(first.minCapacity()).isEqualTo(20);
		assertThat(first.remainingToMin()).isZero();
		assertThat(first.remainingToCapacity()).isEqualTo(6);
		assertThat(first.fillPercent()).isEqualTo(85);
		assertThat(first.daysUntilDeadline()).isEqualTo(5);
		assertThat(first.deadlinePassed()).isFalse();

		verify(applicationRepository, times(1)).countByEventIdsExcludingStatus(anyCollection(), any());
	}

	@Test
	void 신청이_없는_행사는_0명으로_계산한다() {
		Event empty = event(20L, "빈 행사", NOW.plusDays(3), 40, 20, NOW.plusDays(2));
		given(eventRepository.findAllByClubIdAndEventAtAfterOrderByEventAtAscIdAsc(1L, NOW))
				.willReturn(List.of(empty));
		given(applicationRepository.countByEventIdsExcludingStatus(List.of(20L), ApplicationStatus.CANCELED))
				.willReturn(List.of());

		EventCardResponse card = eventQueryService.getOngoingEvents(club, NOW).events().get(0);

		assertThat(card.applicantCount()).isZero();
		assertThat(card.status()).isEqualTo(EventProgressStatus.BELOW_MIN);
		assertThat(card.hint()).isEqualTo("필수 20명까지 20명 남음");
		assertThat(card.fillPercent()).isZero();
	}

	@Test
	void 진행_중인_행사가_없으면_신청_인원을_조회하지_않는다() {
		given(eventRepository.findAllByClubIdAndEventAtAfterOrderByEventAtAscIdAsc(1L, NOW))
				.willReturn(List.of());

		EventListResponse response = eventQueryService.getOngoingEvents(club, NOW);

		assertThat(response.count()).isZero();
		assertThat(response.events()).isEmpty();
		verify(applicationRepository, never()).countByEventIdsExcludingStatus(anyCollection(), any());
	}

	@Test
	void 신청_마감이_지난_행사는_마감_여부와_음수_디데이를_내려준다() {
		Event closed = event(30L, "마감 지난 행사", NOW.plusDays(5), 30, 10, NOW.minusDays(1));
		given(eventRepository.findAllByClubIdAndEventAtAfterOrderByEventAtAscIdAsc(1L, NOW))
				.willReturn(List.of(closed));
		given(applicationRepository.countByEventIdsExcludingStatus(List.of(30L), ApplicationStatus.CANCELED))
				.willReturn(List.of(new EventApplicantCount(30L, 12)));

		EventCardResponse card = eventQueryService.getOngoingEvents(club, NOW).events().get(0);

		assertThat(card.deadlinePassed()).isTrue();
		assertThat(card.daysUntilDeadline()).isEqualTo(-1);
		assertThat(card.status()).isEqualTo(EventProgressStatus.AVAILABLE);
	}

	private Event event(Long id, String title, LocalDateTime eventAt, int capacity, int minCapacity,
			LocalDateTime applyDeadline) {
		Event event = Event.builder()
				.club(club)
				.title(title)
				.location(id == 10L ? "공학관 302호" : "[장소]")
				.eventAt(eventAt)
				.capacity(capacity)
				.minCapacity(minCapacity)
				.applyDeadline(applyDeadline)
				.shareCode("share-" + id)
				.build();
		ReflectionTestUtils.setField(event, "id", id);
		return event;
	}
}
