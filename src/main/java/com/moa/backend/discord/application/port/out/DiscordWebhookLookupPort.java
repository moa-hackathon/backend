package com.moa.backend.discord.application.port.out;

import com.moa.backend.discord.domain.DiscordWebhook;

/** Discord에 Webhook이 실제로 있는지 조회하는 출력 포트. */
public interface DiscordWebhookLookupPort {

    /**
     * @return Webhook이 있고 토큰이 맞으면 true, 없거나(삭제됨) 토큰이 틀리면 false
     * @throws IllegalStateException Discord에 확인하지 못했을 때(네트워크 오류, rate limit, 5xx 등)
     */
    boolean exists(DiscordWebhook webhook);
}
