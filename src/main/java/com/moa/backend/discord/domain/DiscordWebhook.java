package com.moa.backend.discord.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 메시지를 보낼 Discord Webhook. URL에서 id와 token만 꺼내 보관한다.
 * 외부 입력 URL을 그대로 호출하지 않기 위해(SSRF 방지) Discord Webhook 형식만 허용한다.
 */
public record DiscordWebhook(String id, String token) {

    private static final Pattern WEBHOOK_URL = Pattern.compile(
            "^https://(?:(?:ptb|canary)\\.)?discord(?:app)?\\.com/api(?:/v\\d+)?/webhooks/(\\d+)/([A-Za-z0-9_-]+)/?(?:\\?.*)?$");
    private static final Pattern ID = Pattern.compile("\\d+");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]+");

    public DiscordWebhook {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Discord Webhook ID 형식이 올바르지 않습니다.");
        }
        if (token == null || !TOKEN.matcher(token).matches()) {
            throw new IllegalArgumentException("Discord Webhook 토큰 형식이 올바르지 않습니다.");
        }
    }

    /**
     * @param url 채널 설정 → 연동 → 웹후크에서 복사한 URL (https://discord.com/api/webhooks/{id}/{token})
     */
    public static DiscordWebhook parse(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Discord Webhook URL이 비어 있습니다.");
        }
        Matcher matcher = WEBHOOK_URL.matcher(url.strip());
        if (!matcher.matches()) {
            // 입력값에 토큰이 들어 있을 수 있으므로 메시지에 URL을 넣지 않는다.
            throw new IllegalArgumentException(
                    "Discord Webhook URL 형식이 아닙니다. https://discord.com/api/webhooks/{id}/{token} 형태여야 합니다.");
        }
        return new DiscordWebhook(matcher.group(1), matcher.group(2));
    }

    /**
     * 저장용 정규화 URL. 입력이 discordapp.com, ptb/canary, /v10, 쿼리 등 어떤 변형이어도
     * https://discord.com/api/webhooks/{id}/{token} 하나로 맞춘다. 토큰이 들어 있으므로 응답·로그에 쓰지 않는다.
     */
    public String canonicalUrl() {
        return "https://discord.com/api/webhooks/" + id + "/" + token;
    }

    /** 로그에 토큰이 찍히지 않도록 가린다. */
    @Override
    public String toString() {
        return "DiscordWebhook[id=" + id + ", token=****]";
    }
}
