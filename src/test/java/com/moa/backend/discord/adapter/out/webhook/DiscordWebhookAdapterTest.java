package com.moa.backend.discord.adapter.out.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.moa.backend.discord.domain.DiscordWebhook;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DiscordWebhookAdapterTest {

    private static final DiscordWebhook WEBHOOK = new DiscordWebhook("123456789012345678", "secret-token_ABC");
    private static final String LOOKUP_URL = "https://discord.com/api/webhooks/123456789012345678/secret-token_ABC";

    private MockRestServiceServer server;
    private DiscordWebhookAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new DiscordWebhookAdapter(builder);
    }

    @Test
    void 웹훅이_있으면_true() {
        server.expect(requestTo(LOOKUP_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"id\":\"123456789012345678\",\"type\":1,\"token\":\"secret-token_ABC\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(adapter.exists(WEBHOOK)).isTrue();
        server.verify();
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"NOT_FOUND", "UNAUTHORIZED", "FORBIDDEN"})
    void 없는_웹훅이거나_토큰이_틀리면_false(HttpStatus status) {
        server.expect(requestTo(LOOKUP_URL)).andRespond(withStatus(status));

        assertThat(adapter.exists(WEBHOOK)).isFalse();
        server.verify();
    }

    @Test
    void 그_밖의_HTTP_오류는_토큰_없이_예외() {
        server.expect(requestTo(LOOKUP_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> adapter.exists(WEBHOOK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("503")
                .hasMessageNotContaining("secret-token")
                .hasNoCause();
    }

    @Test
    void 네트워크_오류는_URL을_담은_원인_예외를_붙이지_않는다() {
        server.expect(requestTo(LOOKUP_URL)).andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> adapter.exists(WEBHOOK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("secret-token")
                .hasNoCause();
    }
}
