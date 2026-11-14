package com.moa.backend.discord.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DiscordWebhookTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://discord.com/api/webhooks/123/tok-en_1",
            "https://discordapp.com/api/webhooks/123/tok-en_1/",
            "https://ptb.discord.com/api/v10/webhooks/123/tok-en_1?wait=true",
            " https://canary.discord.com/api/webhooks/123/tok-en_1 "})
    void 어떤_변형이든_정규화한_URL_하나로_맞춘다(String url) {
        assertThat(DiscordWebhook.parse(url).canonicalUrl())
                .isEqualTo("https://discord.com/api/webhooks/123/tok-en_1");
    }

    @Test
    void toString에는_토큰이_없다() {
        assertThat(DiscordWebhook.parse("https://discord.com/api/webhooks/123/tok-en_1").toString())
                .doesNotContain("tok-en_1");
    }
}
