package com.moa.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.moa.backend.application.dto.CsvUploadResponse;
import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationSource;
import com.moa.backend.application.entity.ApplicationStatus;
import com.moa.backend.application.repository.ApplicationRepository;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApplicationCsvImportServiceTest {

	private static final long EVENT_ID = 10L;
	private static final String HEADER = "타임스탬프,이름,학번,연락처,뒤풀이 참석 여부\n";
	private static final String ROW_A = "2026/11/14 9:12:00 오전 GMT+9,김모아,20241234,010-1111-2222,참석\n";
	private static final String ROW_B = "2026/11/14 9:30:00 오전 GMT+9,이모아,20231515,010-3333-4444,불참\n";
	private static final String ROW_C = "2026/11/14 10:05:00 오전 GMT+9,박모아,20250707,,참석\n";

	@Mock
	private EventRepository eventRepository;

	@Mock
	private ApplicationRepository applicationRepository;

	@InjectMocks
	private ApplicationCsvImportService service;

	@Captor
	private ArgumentCaptor<List<Application>> savedCaptor;

	private Club club;
	private Event event;
	/** 저장소 역할을 하는 메모리 목록 */
	private final List<Application> stored = new ArrayList<>();

	@BeforeEach
	void setUp() {
		club = Club.builder().name("모아").inviteCode("code").build();
		ReflectionTestUtils.setField(club, "id", 1L);
		event = Event.builder()
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
	}

	private void givenEventAndStore() {
		given(eventRepository.findByIdAndClubId(EVENT_ID, 1L)).willReturn(Optional.of(event));
		given(applicationRepository.findRowHashesByEventId(EVENT_ID))
				.willAnswer(inv -> stored.stream().map(Application::getRowHash).toList());
		given(applicationRepository.saveAll(savedCaptor.capture())).willAnswer(inv -> {
			List<Application> apps = inv.getArgument(0);
			for (Application app : apps) {
				ReflectionTestUtils.setField(app, "id", (long) stored.size() + 1);
				stored.add(app);
			}
			return apps;
		});
		given(applicationRepository.findAllByEventIdOrderByAppliedAtAscIdAsc(EVENT_ID))
				.willAnswer(inv -> stored.stream()
						.sorted(Comparator.comparing(Application::getAppliedAt).thenComparing(Application::getId))
						.toList());
	}

	@Test
	void 새_행을_적재하고_정원까지_확정_나머지는_대기() {
		givenEventAndStore();

		CsvUploadResponse response = service.importCsv(club, EVENT_ID, bytes(HEADER + ROW_A + ROW_B + ROW_C));

		assertThat(response.totalRows()).isEqualTo(3);
		assertThat(response.insertedCount()).isEqualTo(3);
		assertThat(response.skippedCount()).isZero();
		assertThat(response.failures()).isEmpty();
		assertThat(response.confirmedCount()).isEqualTo(2);
		assertThat(response.waitingCount()).isEqualTo(1);
		assertThat(response.lastSyncedAt()).isNotNull().isEqualTo(event.getLastSyncedAt());

		Application first = stored.get(0);
		assertThat(first.getEvent()).isSameAs(event);
		assertThat(first.getSource()).isEqualTo(ApplicationSource.CSV);
		assertThat(first.getName()).isEqualTo("김모아");
		assertThat(first.getAppliedAt()).isEqualTo(LocalDateTime.of(2026, 11, 14, 9, 12));
		assertThat(first.getAnswers()).containsEntry("뒤풀이 참석 여부", "참석").hasSize(1);
		assertThat(first.getRowHash()).hasSize(64);
		assertThat(stored).extracting(Application::getStatus).containsExactly(
				ApplicationStatus.CONFIRMED, ApplicationStatus.CONFIRMED, ApplicationStatus.WAITING);
		assertThat(stored.get(2).getContact()).isNull();
	}

	@Test
	void 같은_파일을_다시_올리면_모두_건너뛴다() {
		givenEventAndStore();
		byte[] file = bytes(HEADER + ROW_A + ROW_B);
		service.importCsv(club, EVENT_ID, file);

		CsvUploadResponse again = service.importCsv(club, EVENT_ID, file);

		assertThat(again.totalRows()).isEqualTo(2);
		assertThat(again.insertedCount()).isZero();
		assertThat(again.skippedCount()).isEqualTo(2);
		assertThat(stored).hasSize(2);
		verify(applicationRepository, times(2)).findRowHashesByEventId(EVENT_ID);
	}

	@Test
	void 응답이_늘어난_파일은_새_행만_넣는다() {
		givenEventAndStore();
		service.importCsv(club, EVENT_ID, bytes(HEADER + ROW_A + ROW_B));

		CsvUploadResponse grown = service.importCsv(club, EVENT_ID, bytes(HEADER + ROW_A + ROW_B + ROW_C));

		assertThat(grown.insertedCount()).isEqualTo(1);
		assertThat(grown.skippedCount()).isEqualTo(2);
		assertThat(savedCaptor.getValue()).extracting(Application::getName).containsExactly("박모아");
		assertThat(stored).hasSize(3);
	}

	@Test
	void 파일_안에서_똑같은_행은_한_번만_넣는다() {
		givenEventAndStore();

		CsvUploadResponse response = service.importCsv(club, EVENT_ID, bytes(HEADER + ROW_A + ROW_A));

		assertThat(response.totalRows()).isEqualTo(2);
		assertThat(response.insertedCount()).isEqualTo(1);
		assertThat(response.skippedCount()).isEqualTo(1);
	}

	@Test
	void 같은_학번_재응답은_대기로_두고_취소는_유지한다() {
		givenEventAndStore();
		Application canceled = Application.builder()
				.event(event)
				.name("취소자")
				.studentId("20250707")
				.status(ApplicationStatus.CANCELED)
				.source(ApplicationSource.FORM)
				.appliedAt(LocalDateTime.of(2026, 11, 14, 8, 0))
				.build();
		ReflectionTestUtils.setField(canceled, "id", 99L);
		stored.add(canceled);
		String duplicate = "2026/11/14 11:20:00 오전 GMT+9,김모아,2024 1234,010-1111-2222,불참\n";

		CsvUploadResponse response = service.importCsv(club, EVENT_ID, bytes(HEADER + ROW_A + duplicate + ROW_C));

		assertThat(response.confirmedCount()).isEqualTo(2);
		assertThat(response.waitingCount()).isEqualTo(1);
		assertThat(canceled.getStatus()).isEqualTo(ApplicationStatus.CANCELED);
		assertThat(stored).filteredOn(app -> "김모아".equals(app.getName()))
				.extracting(Application::getStatus)
				.containsExactly(ApplicationStatus.CONFIRMED, ApplicationStatus.WAITING);
		assertThat(stored).filteredOn(app -> "박모아".equals(app.getName()))
				.extracting(Application::getStatus)
				.containsExactly(ApplicationStatus.CONFIRMED);
	}

	@Test
	void 실패한_행은_적재하지_않고_목록으로_돌려준다() {
		givenEventAndStore();

		CsvUploadResponse response = service.importCsv(club, EVENT_ID, bytes(HEADER + ROW_A + "언젠가,이모아,1,2,참석\n"));

		assertThat(response.totalRows()).isEqualTo(2);
		assertThat(response.insertedCount()).isEqualTo(1);
		assertThat(response.failures()).singleElement().satisfies(failure -> {
			assertThat(failure.rowNumber()).isEqualTo(3);
			assertThat(failure.reason()).contains("타임스탬프");
		});
	}

	@Test
	void 헤더만_있으면_0건이고_동기화_시각은_갱신한다() {
		givenEventAndStore();

		CsvUploadResponse response = service.importCsv(club, EVENT_ID, bytes(HEADER));

		assertThat(response.totalRows()).isZero();
		assertThat(response.insertedCount()).isZero();
		assertThat(event.getLastSyncedAt()).isNotNull();
		assertThat(event.getFormQuestions()).containsExactly("뒤풀이 참석 여부");
	}

	@Test
	void 문항_순서를_최신_파일_기준으로_덮어쓴다() {
		givenEventAndStore();
		service.importCsv(club, EVENT_ID, bytes(HEADER + ROW_A));
		assertThat(event.getFormQuestions()).containsExactly("뒤풀이 참석 여부");

		service.importCsv(club, EVENT_ID, bytes("타임스탬프,하고 싶은 말,이름,뒤풀이 참석 여부,,하고 싶은 말\n"));

		assertThat(event.getFormQuestions()).containsExactly("하고 싶은 말", "뒤풀이 참석 여부", "열 5", "하고 싶은 말 (2)");
	}

	@Test
	void 다른_동아리의_행사면_EVENT_NOT_FOUND() {
		given(eventRepository.findByIdAndClubId(anyLong(), anyLong())).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.importCsv(club, EVENT_ID, bytes(HEADER + ROW_A)))
				.isInstanceOf(ApiException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.EVENT_NOT_FOUND);
		verify(applicationRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void 이름_열이_없으면_CSV_COLUMN_MISSING이고_아무것도_바꾸지_않는다() {
		given(eventRepository.findByIdAndClubId(EVENT_ID, 1L)).willReturn(Optional.of(event));

		assertThatThrownBy(() -> service.importCsv(club, EVENT_ID, bytes("타임스탬프,학번\n2026/11/14 9:12:00,1\n")))
				.isInstanceOf(ApiException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.CSV_COLUMN_MISSING);
		assertThat(event.getLastSyncedAt()).isNull();
		assertThat(event.getFormQuestions()).isNull();
		verify(applicationRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void 빈_파일이면_CSV_EMPTY() {
		given(eventRepository.findByIdAndClubId(EVENT_ID, 1L)).willReturn(Optional.of(event));

		assertThatThrownBy(() -> service.importCsv(club, EVENT_ID, new byte[0]))
				.isInstanceOf(ApiException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.CSV_EMPTY);
	}

	private static byte[] bytes(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}
}
