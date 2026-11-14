package com.moa.backend.notice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.auth.CurrentClubArgumentResolver;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.notice.dto.NoticeDraftResponse;
import com.moa.backend.notice.dto.NoticeDraftSource;
import com.moa.backend.notice.service.NoticeDraftService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NoticeDraftController.class)
class NoticeDraftControllerTest {

	private static final String INVITE_CODE = "invite-code";

	private static final String BODY = """
			{
			  "title": "모아 네트워킹 행사",
			  "eventAt": "2026-11-21T19:00:00",
			  "location": "공학관 302호",
			  "capacity": 40,
			  "minCapacity": 20,
			  "applyDeadline": "2026-11-19T23:59:00",
			  "extraQuestions": ["뒤풀이 참석 여부"],
			  "description": "피자 나와요"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NoticeDraftService noticeDraftService;

	@MockitoBean
	private ClubRepository clubRepository;

	@Test
	void 공지_초안을_돌려준다() throws Exception {
		Club club = club();
		given(noticeDraftService.draft(eq(club), argThat(request ->
				request.title().equals("모아 네트워킹 행사")
						&& request.eventAt().equals(LocalDateTime.of(2026, 11, 21, 19, 0))
						&& request.capacity() == 40
						&& request.minCapacity() == 20
						&& request.extraQuestions().equals(List.of("뒤풀이 참석 여부"))
						&& request.description().equals("피자 나와요")
						&& !request.isRevision())))
				.willReturn(new NoticeDraftResponse("공지\n신청 링크: [구글폼 링크]", NoticeDraftSource.AI));

		mockMvc.perform(post("/api/notices/draft")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.noticeText").value("공지\n신청 링크: [구글폼 링크]"))
				.andExpect(jsonPath("$.source").value("AI"));
	}

	@Test
	void 요약이_규칙을_어기면_400() throws Exception {
		Club club = club();
		given(noticeDraftService.draft(eq(club), any()))
				.willThrow(new ApiException(ErrorCode.INVALID_REQUEST, "정원은 1명 이상이어야 합니다."));

		mockMvc.perform(post("/api/notices/draft")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.message").value("정원은 1명 이상이어야 합니다."));
	}

	@Test
	void 수정_요청이_실패하면_502() throws Exception {
		Club club = club();
		given(noticeDraftService.draft(eq(club), any())).willThrow(new ApiException(ErrorCode.AI_RESPONSE_FAILED));

		mockMvc.perform(post("/api/notices/draft")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("AI_RESPONSE_FAILED"));
	}

	@Test
	void 초대_코드가_없으면_401() throws Exception {
		mockMvc.perform(post("/api/notices/draft").contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_REQUIRED"));

		verifyNoInteractions(noticeDraftService);
	}

	@Test
	void 본문_형식이_틀리면_400() throws Exception {
		club();

		mockMvc.perform(post("/api/notices/draft")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventAt\": \"다음 주 금요일\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		verifyNoInteractions(noticeDraftService);
	}

	private Club club() {
		Club club = Club.builder().name("모아").inviteCode(INVITE_CODE).build();
		ReflectionTestUtils.setField(club, "id", 1L);
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));
		return club;
	}
}
