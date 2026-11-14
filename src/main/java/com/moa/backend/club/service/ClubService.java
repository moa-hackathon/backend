package com.moa.backend.club.service;

import com.moa.backend.club.dto.ClubResponse;
import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.discord.application.port.in.VerifyDiscordWebhookUseCase;
import com.moa.backend.discord.domain.DiscordWebhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubService {

	static final String WEBHOOK_NOT_FOUND_MESSAGE = "존재하지 않는 디스코드 웹훅입니다. 웹훅 URL을 다시 확인해 주세요.";
	static final String DISCORD_UNAVAILABLE_MESSAGE = "디스코드에 웹훅을 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.";

	private final ClubRepository clubRepository;
	private final VerifyDiscordWebhookUseCase verifyDiscordWebhookUseCase;

	public ClubResponse getClub(Club club) {
		return ClubResponse.from(club);
	}

	/**
	 * 웹훅 URL 형식을 검사하고 Discord에 실제로 있는 웹훅인지 확인한 뒤 동아리에 등록(이미 있으면 교체)한다.
	 * 외부 호출이 끝난 뒤 저장만 하므로 트랜잭션은 저장(save)에만 걸린다.
	 * 웹훅 URL에는 토큰이 들어 있으므로 로그·응답 어디에도 남기지 않는다.
	 */
	public ClubResponse connectDiscordWebhook(Club club, String webhookUrl) {
		DiscordWebhook webhook = parse(webhookUrl);
		if (!exists(webhook)) {
			throw new ApiException(ErrorCode.DISCORD_WEBHOOK_INVALID, WEBHOOK_NOT_FOUND_MESSAGE);
		}
		club.connectDiscordWebhook(webhook.canonicalUrl());
		return ClubResponse.from(clubRepository.save(club));
	}

	private static DiscordWebhook parse(String webhookUrl) {
		try {
			return DiscordWebhook.parse(webhookUrl);
		} catch (IllegalArgumentException e) {
			throw new ApiException(ErrorCode.DISCORD_WEBHOOK_INVALID);
		}
	}

	private boolean exists(DiscordWebhook webhook) {
		try {
			return verifyDiscordWebhookUseCase.exists(webhook);
		} catch (RuntimeException e) {
			// 원인 예외의 스택에 호출 URL(토큰)이 섞일 수 있어 메시지만 남긴다.
			log.warn("디스코드 웹훅 확인 실패: webhookId={}, reason={}", webhook.id(), e.getMessage());
			throw new ApiException(ErrorCode.DISCORD_SEND_FAILED, DISCORD_UNAVAILABLE_MESSAGE);
		}
	}
}
