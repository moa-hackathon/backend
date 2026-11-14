package com.moa.backend.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.dto.EventCreateRequest;
import com.moa.backend.event.dto.EventCreateResponse;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventCreateServiceTest {

	private static final LocalDateTime EVENT_AT = LocalDateTime.of(2026, 11, 21, 19, 0);
	private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 11, 19, 23, 59);
	private static final String NOTICE = "[모아] 네트워킹 행사 안내\n신청 링크: [구글폼 링크]";

	@Mock
	private EventRepository eventRepository;

	@Mock
	private ShareCodeGenerator shareCodeGenerator;

	@InjectMocks
	private EventCreateService eventCreateService;

	private Club club;

	@BeforeEach
	void setUp() {
		club = Club.builder().name("모아").inviteCode("code").build();
		ReflectionTestUtils.setField(club, "id", 1L);
	}

	@Test
	void 요약과_공지와_추가_질문을_저장하고_공유_코드를_발급한다() {
		given(shareCodeGenerator.generate()).willReturn("share-code");
		given(eventRepository.save(any(Event.class))).willAnswer(invocation -> {
			Event event = invocation.getArgument(0);
			ReflectionTestUtils.setField(event, "id", 10L);
			return event;
		});

		EventCreateResponse response = eventCreateService.create(club,
				request(40, 20, DEADLINE, List.of("뒤풀이 참석 여부", " "), NOTICE));

		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventRepository).save(captor.capture());
		Event saved = captor.getValue();
		assertThat(saved.getClub()).isSameAs(club);
		assertThat(saved.getTitle()).isEqualTo("모아 네트워킹 행사");
		assertThat(saved.getEventAt()).isEqualTo(EVENT_AT);
		assertThat(saved.getLocation()).isEqualTo("공학관 302호");
		assertThat(saved.getCapacity()).isEqualTo(40);
		assertThat(saved.getMinCapacity()).isEqualTo(20);
		assertThat(saved.getApplyDeadline()).isEqualTo(DEADLINE);
		assertThat(saved.getDescription()).isEqualTo("피자 나와요");
		assertThat(saved.getShareCode()).isEqualTo("share-code");
		assertThat(saved.getNoticeText()).isEqualTo(NOTICE);
		assertThat(saved.getExtraQuestions()).containsExactly("뒤풀이 참석 여부");
		assertThat(saved.getFormUrl()).isNull();
		assertThat(saved.getNoticeSentAt()).isNull();

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.shareCode()).isEqualTo("share-code");
		assertThat(response.noticeText()).isEqualTo(NOTICE);
		assertThat(response.extraQuestions()).containsExactly("뒤풀이 참석 여부");
	}

	@Test
	void 공지에_신청_링크_자리가_없으면_붙여서_저장한다() {
		given(shareCodeGenerator.generate()).willReturn("share-code");
		given(eventRepository.save(any(Event.class))).willAnswer(invocation -> invocation.getArgument(0));

		EventCreateResponse response = eventCreateService.create(club,
				request(40, 20, DEADLINE, null, "[모아] 네트워킹 행사 안내"));

		assertThat(response.noticeText()).isEqualTo(NOTICE);
		assertThat(response.extraQuestions()).isEmpty();
	}

	@Test
	void 공지_본문이_없으면_400() {
		assertInvalid(request(40, 20, DEADLINE, null, "  "));
	}

	@Test
	void 정원이_1명_미만이면_400() {
		assertInvalid(request(0, 0, DEADLINE, null, NOTICE));
	}

	@Test
	void 최소_인원이_정원을_넘으면_400() {
		assertInvalid(request(40, 41, DEADLINE, null, NOTICE));
	}

	@Test
	void 신청_마감이_행사_일시보다_늦으면_400() {
		assertInvalid(request(40, 20, EVENT_AT.plusDays(1), null, NOTICE));
	}

	private void assertInvalid(EventCreateRequest request) {
		assertThatThrownBy(() -> eventCreateService.create(club, request))
				.isInstanceOf(ApiException.class)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
		verifyNoInteractions(eventRepository, shareCodeGenerator);
	}

	private static EventCreateRequest request(int capacity, int minCapacity, LocalDateTime deadline,
			List<String> extraQuestions, String noticeText) {
		return new EventCreateRequest("모아 네트워킹 행사", EVENT_AT, "공학관 302호", capacity, minCapacity, deadline,
				extraQuestions, "피자 나와요", noticeText);
	}
}
