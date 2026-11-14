package com.moa.backend.discord.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Discord로 보낼 메시지 한 건. Discord는 메시지 한 건당 최대 2000자까지 허용한다.
 */
public record DiscordMessage(String content) {

    public static final int MAX_LENGTH = 2000;

    public DiscordMessage {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Discord 메시지 내용이 비어 있습니다.");
        }
        if (content.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Discord 메시지는 %d자를 넘을 수 없습니다. (현재 %d자)".formatted(MAX_LENGTH, content.length()));
        }
    }

    /**
     * 긴 글을 2000자 이하 메시지 여러 개로 나눈다.
     * 가능하면 줄바꿈 위치에서 자르고, 줄바꿈이 없으면 2000자에서 자른다(서로게이트 쌍은 쪼개지 않는다).
     */
    public static List<DiscordMessage> split(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Discord 메시지 내용이 비어 있습니다.");
        }
        List<DiscordMessage> messages = new ArrayList<>();
        String remaining = text;
        while (remaining.length() > MAX_LENGTH) {
            int cut = remaining.lastIndexOf('\n', MAX_LENGTH);
            int next;
            if (cut > 0) {
                next = cut + 1; // 자른 위치의 줄바꿈은 버린다
            } else {
                cut = MAX_LENGTH;
                if (Character.isHighSurrogate(remaining.charAt(cut - 1))) {
                    cut--;
                }
                next = cut;
            }
            addIfNotBlank(messages, remaining.substring(0, cut));
            remaining = remaining.substring(next);
        }
        addIfNotBlank(messages, remaining);
        return List.copyOf(messages);
    }

    private static void addIfNotBlank(List<DiscordMessage> messages, String chunk) {
        if (!chunk.isBlank()) {
            messages.add(new DiscordMessage(chunk));
        }
    }
}
