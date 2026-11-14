package com.moa.backend.discord.application.port.out;

import com.moa.backend.discord.domain.DiscordMessage;
import com.moa.backend.discord.domain.DiscordWebhook;

/** Discord로 메시지를 실제 전송하는 곳(Webhook API)에 대한 출력 포트. */
public interface DiscordMessagePort {

    /** @return Discord가 발급한 메시지 ID */
    String send(DiscordWebhook webhook, DiscordMessage message);
}
