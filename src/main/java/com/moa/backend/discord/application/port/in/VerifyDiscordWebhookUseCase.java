package com.moa.backend.discord.application.port.in;

import com.moa.backend.discord.domain.DiscordWebhook;

public interface VerifyDiscordWebhookUseCase {

    /**
     * Discord에 실제로 있는 Webhook인지 확인한다. 메시지는 보내지 않는다.
     *
     * @return Webhook이 있고 토큰이 맞으면 true, 없거나 토큰이 틀리면 false
     * @throws IllegalStateException Discord에 확인하지 못했을 때
     */
    boolean exists(DiscordWebhook webhook);
}
