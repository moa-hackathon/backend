package com.moa.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.moa.backend.club.dto.ClubResponse;
import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.discord.application.port.in.VerifyDiscordWebhookUseCase;
import com.moa.backend.discord.domain.DiscordWebhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

	private static final DiscordWebhook WEBHOOK = new DiscordWebhook("123456789012345678", "secret-token_ABC");
	private static final String CANONICAL_URL = "https://discord.com/api/webhooks/123456789012345678/secret-token_ABC";

	@Mock
	private ClubRepository clubRepository;

	@Mock
	private VerifyDiscordWebhookUseCase verifyDiscordWebhookUseCase;

	@InjectMocks
	private ClubService clubService;

	private Club club;

	@BeforeEach
	void setUp() {
		club = Club.builder().name("모아").description("IT 연합 동아리").inviteCode("code").build();
		ReflectionTestUtils.setField(club, "id", 1L);
	}

	@Test
	void 동아리_정보를_돌려준다() {
		ClubResponse response = clubService.getClub(club);

		assertThat(response).isEqualTo(new ClubResponse(1L, "모아", "IT 연합 동아리", false));
	}

	@Test
	void 존재하는_웹훅이면_정규화한_URL로_저장하고_연결됨으로_돌려준다() {
		given(verifyDiscordWebhookUseCase.exists(WEBHOOK)).willReturn(true);
		given(clubRepository.save(club)).willReturn(club);

		ClubResponse response = clubService.connectDiscordWebhook(club,
				"  https://discordapp.com/api/v10/webhooks/123456789012345678/secret-token_ABC?wait=true  ");

		assertThat(club.getDiscordWebhookUrl()).isEqualTo(CANONICAL_URL);
		assertThat(response.discordConnected()).isTrue();
		verify(clubRepository).save(club);
	}

	@Test
	void 이미_연결돼_있으면_새_웹훅으로_교체한다() {
		club.connectDiscordWebhook("https://discord.com/api/webhooks/1/old-token");
		given(verifyDiscordWebhookUseCase.exists(WEBHOOK)).willReturn(true);
		given(clubRepository.save(club)).willReturn(club);

		clubService.connectDiscordWebhook(club, CANONICAL_URL);

		assertThat(club.getDiscordWebhookUrl()).isEqualTo(CANONICAL_URL);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "https://example.com/api/webhooks/1/token", "http://discord.com/api/webhooks/1/token",
			"https://discord.com/api/webhooks/abc/token", "디스코드 봇 토큰"})
	void 형식이_틀리면_Discord를_부르지_않고_DISCORD_WEBHOOK_INVALID(String webhookUrl) {
		assertThatThrownBy(() -> clubService.connectDiscordWebhook(club, webhookUrl))
				.isInstanceOfSatisfying(ApiException.class, e -> {
					assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DISCORD_WEBHOOK_INVALID);
					assertThat(e.getMessage()).isEqualTo(ErrorCode.DISCORD_WEBHOOK_INVALID.getMessage());
				});

		verifyNoInteractions(verifyDiscordWebhookUseCase, clubRepository);
		assertThat(club.isDiscordConnected()).isFalse();
	}

	@Test
	void Discord에_없는_웹훅이면_저장하지_않고_DISCORD_WEBHOOK_INVALID() {
		given(verifyDiscordWebhookUseCase.exists(WEBHOOK)).willReturn(false);

		assertThatThrownBy(() -> clubService.connectDiscordWebhook(club, CANONICAL_URL))
				.isInstanceOfSatisfying(ApiException.class, e -> {
					assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DISCORD_WEBHOOK_INVALID);
					assertThat(e.getMessage()).isEqualTo(ClubService.WEBHOOK_NOT_FOUND_MESSAGE);
				});

		verify(clubRepository, never()).save(any());
		assertThat(club.isDiscordConnected()).isFalse();
	}

	@Test
	void Discord에_확인하지_못하면_저장하지_않고_502() {
		given(verifyDiscordWebhookUseCase.exists(WEBHOOK))
				.willThrow(new IllegalStateException("Discord Webhook 조회에 실패했습니다 (HTTP 503)."));

		assertThatThrownBy(() -> clubService.connectDiscordWebhook(club, CANONICAL_URL))
				.isInstanceOfSatisfying(ApiException.class, e -> {
					assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DISCORD_SEND_FAILED);
					assertThat(e.getMessage()).isEqualTo(ClubService.DISCORD_UNAVAILABLE_MESSAGE);
				});

		verify(clubRepository, never()).save(any());
	}
}
