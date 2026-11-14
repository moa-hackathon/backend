package com.moa.backend.discord.application;

import com.moa.backend.discord.application.port.in.SendDiscordMessageUseCase;
import com.moa.backend.discord.application.port.out.DiscordMessagePort;
import com.moa.backend.discord.domain.DiscordMessage;
import com.moa.backend.discord.domain.DiscordSendResult;
import com.moa.backend.discord.domain.DiscordWebhook;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DiscordService implements SendDiscordMessageUseCase {

    /** 한 번의 요청으로 보낼 수 있는 최대 메시지 수. 채널 도배와 rate limit을 막는다. */
    static final int MAX_MESSAGES_PER_SEND = 10;

    private final DiscordMessagePort discordMessagePort;

    public DiscordService(DiscordMessagePort discordMessagePort) {
        this.discordMessagePort = discordMessagePort;
    }

    @Override
    public DiscordSendResult send(String webhookUrl, String content) {
        DiscordWebhook webhook = DiscordWebhook.parse(webhookUrl);
        List<DiscordMessage> messages = DiscordMessage.split(content);
        if (messages.size() > MAX_MESSAGES_PER_SEND) {
            throw new IllegalArgumentException(
                    "글이 너무 깁니다. 최대 %d개 메시지(약 %d자)까지 보낼 수 있습니다. (필요: %d개)"
                            .formatted(MAX_MESSAGES_PER_SEND, MAX_MESSAGES_PER_SEND * DiscordMessage.MAX_LENGTH,
                                    messages.size()));
        }

        List<String> messageIds = new ArrayList<>();
        for (DiscordMessage message : messages) {
            try {
                messageIds.add(discordMessagePort.send(webhook, message));
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "Discord 발송 실패: 전체 %d개 중 %d개를 보낸 뒤 실패했습니다. 원인: %s"
                                .formatted(messages.size(), messageIds.size(), e.getMessage()), e);
            }
        }
        return new DiscordSendResult(messageIds.size(), messageIds);
    }
}
