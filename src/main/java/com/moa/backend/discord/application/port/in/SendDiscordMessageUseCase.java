package com.moa.backend.discord.application.port.in;

import com.moa.backend.discord.domain.DiscordSendResult;

public interface SendDiscordMessageUseCase {

    /**
     * 글을 Discord로 보낸다. 2000자를 넘으면 여러 메시지로 나눠 순서대로 보낸다.
     *
     * @param webhookUrl 보낼 채널의 Webhook URL (토큰 포함). https://discord.com/api/webhooks/{id}/{token}
     * @param content    보낼 내용
     */
    DiscordSendResult send(String webhookUrl, String content);
}
