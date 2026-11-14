package com.moa.backend.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.dto.FormLinkResponse;
import com.moa.backend.event.dto.FormQuestionResponse;
import com.moa.backend.event.dto.FormQuestionsResponse;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import com.moa.backend.googleform.application.port.in.ReadPublicFormUseCase;
import com.moa.backend.googleform.domain.FormLinkException;
import com.moa.backend.googleform.domain.PublicForm;
import com.moa.backend.googleform.domain.PublicForm.PublicFormItem;
import com.moa.backend.googleform.domain.PublicForm.PublicFormItemType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class FormLinkServiceTest {

	private static final String FORM_URL = "https://forms.gle/AbCdEf123";

	@Mock
	private EventRepository eventRepository;

	@Mock
	private ReadPublicFormUseCase readPublicFormUseCase;

	private FormLinkService formLinkService;

	private Club club;

	@BeforeEach
	void setUp() {
		formLinkService = new FormLinkService(eventRepository, readPublicFormUseCase,
				TransactionOperations.withoutTransaction());
		club = Club.builder().name("모아").inviteCode("code").build();
		ReflectionTestUtils.setField(club, "id", 1L);
	}

	@Test
	void 폼_링크를_확인하면_행사에_저장하고_응답_항목을_돌려준다() {
		Event event = event(List.of("뒤풀이 참석 여부"));
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event));
		given(readPublicFormUseCase.read(" " + FORM_URL + " ")).willReturn(new PublicForm("모아 네트워킹 행사 신청", List.of(
				new PublicFormItem("이름", true, PublicFormItemType.SHORT_ANSWER),
				new PublicFormItem("학번", true, PublicFormItemType.SHORT_ANSWER),
				new PublicFormItem("연락처", true, PublicFormItemType.SHORT_ANSWER),
				new PublicFormItem("뒤풀이 참석 여부", false, PublicFormItemType.MULTIPLE_CHOICE))));

		FormLinkResponse response = formLinkService.connectForm(club, 10L, " " + FORM_URL + " ");

		assertThat(event.getFormUrl()).isEqualTo(FORM_URL);
		assertThat(response.eventId()).isEqualTo(10L);
		assertThat(response.formUrl()).isEqualTo(FORM_URL);
		assertThat(response.formTitle()).isEqualTo("모아 네트워킹 행사 신청");
		assertThat(response.questions())
				.extracting(FormQuestionResponse::title, FormQuestionResponse::required)
				.containsExactly(tuple("이름", true), tuple("학번", true), tuple("연락처", true),
						tuple("뒤풀이 참석 여부", false));
		assertThat(response.message()).isEqualTo("링크를 확인했어요. 응답 항목: 이름, 학번, 연락처, 뒤풀이 참석 여부");
	}

	@Test
	void 폼을_읽지_못하면_저장하지_않고_FORM_LINK_INVALID() {
		Event event = event(null);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event));
		given(readPublicFormUseCase.read(FORM_URL)).willThrow(new FormLinkException("로그인해야 열리는 폼이에요."));

		assertThatThrownBy(() -> formLinkService.connectForm(club, 10L, FORM_URL))
				.isInstanceOf(ApiException.class)
				.hasMessage("로그인해야 열리는 폼이에요.")
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.FORM_LINK_INVALID);
		assertThat(event.getFormUrl()).isNull();
	}

	@Test
	void 다른_동아리_행사면_폼을_열지_않고_EVENT_NOT_FOUND() {
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> formLinkService.connectForm(club, 10L, FORM_URL))
				.isInstanceOf(ApiException.class)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.EVENT_NOT_FOUND);
		verify(readPublicFormUseCase, never()).read(any());
	}

	@Test
	void 문항이_없는_폼도_연결하고_안내한다() {
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event(null)));
		given(readPublicFormUseCase.read(FORM_URL)).willReturn(new PublicForm(null, List.of()));

		FormLinkResponse response = formLinkService.connectForm(club, 10L, FORM_URL);

		assertThat(response.questions()).isEmpty();
		assertThat(response.message()).isEqualTo("링크를 확인했어요. 폼에 응답 항목이 아직 없어요.");
	}

	@Test
	void 추천_문항은_기본_필수_세_개와_추가_질문_선택() {
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event(List.of("뒤풀이 참석 여부"))));

		FormQuestionsResponse response = formLinkService.recommendQuestions(club, 10L);

		assertThat(response.questions())
				.extracting(FormQuestionResponse::title, FormQuestionResponse::required)
				.containsExactly(tuple("이름", true), tuple("학번", true), tuple("연락처", true),
						tuple("뒤풀이 참석 여부", false));
		assertThat(response.copyText()).isEqualTo("1. 이름 (필수)\n2. 학번 (필수)\n3. 연락처 (필수)\n4. 뒤풀이 참석 여부 (선택)");
	}

	@Test
	void 추가_질문이_없으면_기본_문항만() {
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event(null)));

		FormQuestionsResponse response = formLinkService.recommendQuestions(club, 10L);

		assertThat(response.questions()).extracting(FormQuestionResponse::title).containsExactly("이름", "학번", "연락처");
	}

	@Test
	void 추가_질문의_빈_값과_중복은_뺀다() {
		given(eventRepository.findByIdAndClubId(10L, 1L))
				.willReturn(Optional.of(event(Arrays.asList(" 티셔츠 사이즈 ", "", null, "이름", "티셔츠 사이즈"))));

		FormQuestionsResponse response = formLinkService.recommendQuestions(club, 10L);

		assertThat(response.questions())
				.extracting(FormQuestionResponse::title, FormQuestionResponse::required)
				.containsExactly(tuple("이름", true), tuple("학번", true), tuple("연락처", true),
						tuple("티셔츠 사이즈", false));
	}

	@Test
	void 추천_문항도_다른_동아리_행사면_EVENT_NOT_FOUND() {
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> formLinkService.recommendQuestions(club, 10L))
				.isInstanceOf(ApiException.class)
				.extracting(e -> ((ApiException) e).getErrorCode())
				.isEqualTo(ErrorCode.EVENT_NOT_FOUND);
	}

	private Event event(List<String> extraQuestions) {
		Event event = Event.builder()
				.club(club)
				.title("모아 네트워킹 행사")
				.location("공학관 302호")
				.eventAt(LocalDateTime.of(2026, 11, 21, 19, 0))
				.capacity(40)
				.minCapacity(20)
				.applyDeadline(LocalDateTime.of(2026, 11, 19, 23, 59))
				.shareCode("share")
				.extraQuestions(extraQuestions)
				.build();
		ReflectionTestUtils.setField(event, "id", 10L);
		return event;
	}
}
