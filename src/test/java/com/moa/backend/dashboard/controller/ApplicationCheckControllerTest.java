package com.moa.backend.dashboard.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.dashboard.dto.ApplicationCancelResponse;
import com.moa.backend.dashboard.service.ApplicationCheckService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApplicationCheckController.class)
class ApplicationCheckControllerTest {

	private static final String INVITE_CODE = "invite-code";

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private ApplicationCheckService applicationCheckService;
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
	void 중복_신청을_정리한다() throws Exception {
		given(applicationCheckService.resolveDuplicates(club, 10L))
				.willReturn(new ApplicationCancelResponse(2, 32, 0));

		mockMvc.perform(post("/api/events/10/applications/duplicates/resolve").header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.canceledCount").value(2))
				.andExpect(jsonPath("$.confirmedCount").value(32))
				.andExpect(jsonPath("$.waitingCount").value(0));
	}

	@Test
	void 신청_한_건을_취소한다() throws Exception {
		given(applicationCheckService.cancel(club, 10L, 4L))
				.willReturn(new ApplicationCancelResponse(1, 33, 0));

		mockMvc.perform(patch("/api/events/10/applications/4/cancel").header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.canceledCount").value(1));
	}

	@Test
	void 없는_신청이면_404로_응답한다() throws Exception {
		given(applicationCheckService.cancel(club, 10L, 999L))
				.willThrow(new ApiException(ErrorCode.APPLICATION_NOT_FOUND));

		mockMvc.perform(patch("/api/events/10/applications/999/cancel").header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
	}

	@Test
	void 초대_코드가_없으면_401로_응답한다() throws Exception {
		mockMvc.perform(post("/api/events/10/applications/duplicates/resolve"))
				.andExpect(status().isUnauthorized());
	}
}
