package com.moa.backend.discord.adapter.in.mcp;

import com.moa.backend.discord.application.port.in.SendDiscordMessageUseCase;
import com.moa.backend.discord.domain.DiscordSendResult;
import com.moa.backend.mcp.McpToolSet;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Discord 발송 기능을 MCP 도구로 노출하는 인바운드 어댑터. */
@Component
public class DiscordMcpTools implements McpToolSet {

    private final SendDiscordMessageUseCase sendDiscordMessageUseCase;

    public DiscordMcpTools(SendDiscordMessageUseCase sendDiscordMessageUseCase) {
        this.sendDiscordMessageUseCase = sendDiscordMessageUseCase;
    }

    @Tool(name = "send_discord_message",
            description = "Webhook URL로 Discord 채널에 메시지를 보낸다. "
                    + "Markdown을 쓸 수 있다. 2000자를 넘으면 줄 단위로 나눠 여러 메시지로 보낸다(최대 10개). "
                    + "@everyone, @here, 사용자·역할 멘션은 알림이 가지 않는다.")
    public DiscordSendResult sendDiscordMessage(
            @ToolParam(description = "보낼 채널의 Webhook URL (https://discord.com/api/webhooks/{id}/{token})")
            String webhookUrl,
            @ToolParam(description = "보낼 메시지 내용") String content) {
        return sendDiscordMessageUseCase.send(webhookUrl, content);
    }
}
