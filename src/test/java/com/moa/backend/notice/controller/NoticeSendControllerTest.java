package com.moa.backend.notice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.auth.CurrentClubArgumentResolver;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.notice.dto.FinalNoticeResponse;
import com.moa.backend.notice.dto.NoticeSendResponse;
import com.moa.backend.notice.service.NoticeSendService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NoticeSendController.class)
class NoticeSendControllerTest {

	private static final String INVITE_CODE = "invite-code";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NoticeSendService noticeSendService;

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
	void 최종_공지를_돌려준다() throws Exception {
		given(noticeSendService.getFinalNotice(club, 10L)).willReturn(new FinalNoticeResponse(10L,
				"신청 링크: https://forms.gle/abc", "https://forms.gle/abc", false, true,
				LocalDateTime.of(2026, 11, 14, 13, 0)));

		mockMvc.perform(get("/api/events/10/notice").header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventId").value(10))
				.andExpect(jsonPath("$.noticeText").value("신청 링크: https://forms.gle/abc"))
				.andExpect(jsonPath("$.formUrl").value("https://forms.gle/abc"))
				.andExpect(jsonPath("$.formLinkMissing").value(false))
				.andExpect(jsonPath("$.discordConnected").value(true))
				.andExpect(jsonPath("$.sentAt").value("2026-11-14T13:00:00"));
	}

	@Test
	void 다른_동아리_행사면_404() throws Exception {
		given(noticeSendService.getFinalNotice(club, 99L)).willThrow(new ApiException(ErrorCode.EVENT_NOT_FOUND));

		mockMvc.perform(get("/api/events/99/notice").header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
	}

	@Test
	void 공지를_발송한다() throws Exception {
		given(noticeSendService.sendToDiscord(eq(club), eq(10L), any(LocalDateTime.class)))
				.willReturn(new NoticeSendResponse(10L, "DISCORD", LocalDateTime.of(2026, 11, 14, 13, 0), 1));

		mockMvc.perform(post("/api/events/10/notice/send")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventId").value(10))
				.andExpect(jsonPath("$.channel").value("DISCORD"))
				.andExpect(jsonPath("$.sentAt").value("2026-11-14T13:00:00"))
				.andExpect(jsonPath("$.messageCount").value(1));
	}

	@Test
	void 웹훅이_없으면_409() throws Exception {
		given(noticeSendService.sendToDiscord(eq(club), eq(10L), any(LocalDateTime.class)))
				.willThrow(new ApiException(ErrorCode.DISCORD_NOT_CONNECTED));

		mockMvc.perform(post("/api/events/10/notice/send")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DISCORD_NOT_CONNECTED"));
	}

	@Test
	void 발송에_실패하면_502() throws Exception {
		given(noticeSendService.sendToDiscord(eq(club), eq(10L), any(LocalDateTime.class)))
				.willThrow(new ApiException(ErrorCode.DISCORD_SEND_FAILED));

		mockMvc.perform(post("/api/events/10/notice/send")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("DISCORD_SEND_FAILED"));
	}

	@Test
	void 초대_코드가_없으면_401() throws Exception {
		mockMvc.perform(post("/api/events/10/notice/send"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_REQUIRED"));

		verifyNoInteractions(noticeSendService);
	}
}
