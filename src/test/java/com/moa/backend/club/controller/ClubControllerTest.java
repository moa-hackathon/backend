package com.moa.backend.club.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.backend.club.dto.ClubResponse;
import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.club.service.ClubService;
import com.moa.backend.common.auth.CurrentClubArgumentResolver;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClubController.class)
class ClubControllerTest {

	private static final String INVITE_CODE = "invite-code";
	private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/123456789012345678/secret-token_ABC";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ClubService clubService;

	@MockitoBean
	private ClubRepository clubRepository;

	private Club club;

	@BeforeEach
	void setUp() {
		club = Club.builder().name("모아").description("IT 연합 동아리").inviteCode(INVITE_CODE).build();
		ReflectionTestUtils.setField(club, "id", 1L);
	}

	@Test
	void 동아리_정보와_디스코드_연결_여부를_돌려준다() throws Exception {
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));
		given(clubService.getClub(club)).willReturn(new ClubResponse(1L, "모아", "IT 연합 동아리", false));

		mockMvc.perform(get("/api/club").header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("모아"))
				.andExpect(jsonPath("$.description").value("IT 연합 동아리"))
				.andExpect(jsonPath("$.discordConnected").value(false))
				.andExpect(jsonPath("$.discordWebhookUrl").doesNotExist());
	}

	@Test
	void 웹훅을_연결하면_연결된_동아리_정보를_돌려주고_URL은_내려주지_않는다() throws Exception {
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));
		given(clubService.connectDiscordWebhook(club, WEBHOOK_URL))
				.willReturn(new ClubResponse(1L, "모아", "IT 연합 동아리", true));

		mockMvc.perform(put("/api/club/discord-webhook")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"webhookUrl\":\"" + WEBHOOK_URL + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.discordConnected").value(true))
				.andExpect(content().string(Matchers.not(Matchers.containsString("secret-token"))));
	}

	@Test
	void 잘못된_웹훅이면_400() throws Exception {
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));
		given(clubService.connectDiscordWebhook(eq(club), any()))
				.willThrow(new ApiException(ErrorCode.DISCORD_WEBHOOK_INVALID));

		mockMvc.perform(put("/api/club/discord-webhook")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"webhookUrl\":\"https://example.com/hook\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("DISCORD_WEBHOOK_INVALID"))
				.andExpect(jsonPath("$.message").value(ErrorCode.DISCORD_WEBHOOK_INVALID.getMessage()));
	}

	@Test
	void 본문이_없으면_400() throws Exception {
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));

		mockMvc.perform(put("/api/club/discord-webhook")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		verifyNoInteractions(clubService);
	}

	@Test
	void 초대_코드가_없으면_401() throws Exception {
		mockMvc.perform(get("/api/club"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_REQUIRED"));
		mockMvc.perform(put("/api/club/discord-webhook")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"webhookUrl\":\"" + WEBHOOK_URL + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_REQUIRED"));

		verifyNoInteractions(clubService);
	}

	@Test
	void 초대_코드가_틀리면_401() throws Exception {
		given(clubRepository.findByInviteCode("wrong")).willReturn(Optional.empty());

		mockMvc.perform(put("/api/club/discord-webhook")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, "wrong")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"webhookUrl\":\"" + WEBHOOK_URL + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_INVALID"));

		verifyNoInteractions(clubService);
	}
}
