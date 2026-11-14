package com.moa.backend.discord.domain;

import java.util.List;

/** @param messageIds Discord가 발급한 메시지 ID. 나눠 보낸 경우 보낸 순서대로 담긴다. */
public record DiscordSendResult(int messageCount, List<String> messageIds) {

    public DiscordSendResult {
        messageIds = List.copyOf(messageIds);
    }
}
