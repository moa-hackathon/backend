package com.moa.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "디스코드 웹훅 연결 요청")
public record DiscordWebhookConnectRequest(
		@Schema(description = "디스코드 채널 설정 › 연동 › 웹후크에서 복사한 URL",
				example = "https://discord.com/api/webhooks/123456789012345678/abcdefg")
		String webhookUrl
) {

	/** 토큰이 들어 있는 URL이 로그에 찍히지 않도록 가린다. */
	@Override
	public String toString() {
		return "DiscordWebhookConnectRequest[webhookUrl=****]";
	}
}
