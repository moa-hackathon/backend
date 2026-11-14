package com.moa.backend.event.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.auth.CurrentClubArgumentResolver;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.dto.FormLinkResponse;
import com.moa.backend.event.dto.FormQuestionResponse;
import com.moa.backend.event.dto.FormQuestionsResponse;
import com.moa.backend.event.service.FormLinkService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FormLinkController.class)
class FormLinkControllerTest {

	private static final String INVITE_CODE = "invite-code";
	private static final String FORM_URL = "https://forms.gle/AbCdEf123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FormLinkService formLinkService;

	@MockitoBean
	private ClubRepository clubRepository;

	private Club club;

	@BeforeEach
	void setUp() {
		club = Club.builder().name("모아").inviteCode(INVITE_CODE).build();
		ReflectionTestUtils.setField(club, "id", 1L);
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));
	}

	@Test
	void 폼_링크를_확인하고_응답_항목을_돌려준다() throws Exception {
		given(formLinkService.connectForm(club, 10L, FORM_URL)).willReturn(FormLinkResponse.of(10L, FORM_URL,
				"모아 네트워킹 행사 신청", List.of(new FormQuestionResponse("이름", true),
						new FormQuestionResponse("뒤풀이 참석 여부", false))));

		mockMvc.perform(put("/api/events/10/form-link")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"formUrl\":\"" + FORM_URL + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventId").value(10))
				.andExpect(jsonPath("$.formUrl").value(FORM_URL))
				.andExpect(jsonPath("$.formTitle").value("모아 네트워킹 행사 신청"))
				.andExpect(jsonPath("$.questions[0].title").value("이름"))
				.andExpect(jsonPath("$.questions[0].required").value(true))
				.andExpect(jsonPath("$.questions[1].title").value("뒤풀이 참석 여부"))
				.andExpect(jsonPath("$.questions[1].required").value(false))
				.andExpect(jsonPath("$.message").value("링크를 확인했어요. 응답 항목: 이름, 뒤풀이 참석 여부"));
	}

	@Test
	void 확인할_수_없는_폼이면_400_FORM_LINK_INVALID와_안내_문구() throws Exception {
		given(formLinkService.connectForm(eq(club), eq(10L), any()))
				.willThrow(new ApiException(ErrorCode.FORM_LINK_INVALID, "로그인해야 열리는 폼이에요."));

		mockMvc.perform(put("/api/events/10/form-link")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"formUrl\":\"" + FORM_URL + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("FORM_LINK_INVALID"))
				.andExpect(jsonPath("$.message").value("로그인해야 열리는 폼이에요."));
	}

	@Test
	void 링크가_비어_있으면_400() throws Exception {
		mockMvc.perform(put("/api/events/10/form-link")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"formUrl\":\" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		verifyNoInteractions(formLinkService);
	}

	@Test
	void 다른_동아리_행사면_404() throws Exception {
		given(formLinkService.connectForm(eq(club), eq(99L), any()))
				.willThrow(new ApiException(ErrorCode.EVENT_NOT_FOUND));

		mockMvc.perform(put("/api/events/99/form-link")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"formUrl\":\"" + FORM_URL + "\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
	}

	@Test
	void 초대_코드가_없으면_401() throws Exception {
		mockMvc.perform(put("/api/events/10/form-link")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"formUrl\":\"" + FORM_URL + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_REQUIRED"));
		mockMvc.perform(get("/api/events/10/form-questions"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_REQUIRED"));

		verifyNoInteractions(formLinkService);
	}

	@Test
	void 추천_문항을_돌려준다() throws Exception {
		given(formLinkService.recommendQuestions(club, 10L)).willReturn(FormQuestionsResponse.of(List.of(
				new FormQuestionResponse("이름", true),
				new FormQuestionResponse("학번", true),
				new FormQuestionResponse("연락처", true),
				new FormQuestionResponse("뒤풀이 참석 여부", false))));

		mockMvc.perform(get("/api/events/10/form-questions")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions.length()").value(4))
				.andExpect(jsonPath("$.questions[0].title").value("이름"))
				.andExpect(jsonPath("$.questions[0].required").value(true))
				.andExpect(jsonPath("$.questions[3].title").value("뒤풀이 참석 여부"))
				.andExpect(jsonPath("$.questions[3].required").value(false))
				.andExpect(jsonPath("$.copyText")
						.value("1. 이름 (필수)\n2. 학번 (필수)\n3. 연락처 (필수)\n4. 뒤풀이 참석 여부 (선택)"));
	}

	@Test
	void 추천_문항_행사가_없으면_404() throws Exception {
		given(formLinkService.recommendQuestions(club, 99L)).willThrow(new ApiException(ErrorCode.EVENT_NOT_FOUND));

		mockMvc.perform(get("/api/events/99/form-questions")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
	}
}
