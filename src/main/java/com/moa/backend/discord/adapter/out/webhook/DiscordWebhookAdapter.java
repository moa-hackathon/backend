package com.moa.backend.discord.adapter.out.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.backend.discord.application.port.out.DiscordMessagePort;
import com.moa.backend.discord.domain.DiscordMessage;
import com.moa.backend.discord.domain.DiscordWebhook;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Discord Webhook으로 메시지를 보내는 어댑터.
 * 호출 주소는 입력 URL이 아니라 Webhook id/token으로 직접 조립한다(항상 discord.com).
 * 오류 메시지에는 Webhook URL(토큰 포함)을 절대 넣지 않는다.
 */
@Component
public class DiscordWebhookAdapter implements DiscordMessagePort {

    static final String WEBHOOK_BASE_URL = "https://discord.com/api/webhooks";

    /** 429 응답의 Retry-After가 이보다 길면 기다리지 않고 실패시킨다. */
    private static final Duration MAX_RETRY_WAIT = Duration.ofSeconds(5);

    private final RestClient restClient;

    public DiscordWebhookAdapter(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String send(DiscordWebhook webhook, DiscordMessage message) {
        URI uri = webhookUri(webhook);
        WebhookRequest request = new WebhookRequest(message.content(), new AllowedMentions(List.of()));
        try {
            return post(uri, request);
        } catch (HttpClientErrorException.TooManyRequests e) {
            Duration wait = retryAfter(e);
            if (wait.compareTo(MAX_RETRY_WAIT) > 0) {
                throw new IllegalStateException(
                        "Discord rate limit에 걸렸습니다. %d초 후 다시 시도하세요.".formatted(wait.toSeconds()), e);
            }
            sleep(wait);
            return postOrFail(uri, request);
        } catch (RestClientResponseException e) {
            throw failure(e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Discord Webhook 호출에 실패했습니다: " + e.getClass().getSimpleName(), e);
        }
    }

    private String postOrFail(URI uri, WebhookRequest request) {
        try {
            return post(uri, request);
        } catch (RestClientResponseException e) {
            throw failure(e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Discord Webhook 호출에 실패했습니다: " + e.getClass().getSimpleName(), e);
        }
    }

    private String post(URI uri, WebhookRequest request) {
        WebhookMessageResponse response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(WebhookMessageResponse.class);
        if (response == null || response.id() == null) {
            throw new IllegalStateException("Discord 응답에 메시지 ID가 없습니다.");
        }
        return response.id();
    }

    /** wait=true를 붙여야 Discord가 204 대신 생성된 메시지(ID 포함)를 돌려준다. */
    private static URI webhookUri(DiscordWebhook webhook) {
        return UriComponentsBuilder.fromUriString(WEBHOOK_BASE_URL)
                .pathSegment(webhook.id(), webhook.token())
                .queryParam("wait", "true")
                .build()
                .encode()
                .toUri();
    }

    private static Duration retryAfter(RestClientResponseException e) {
        String header = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
        if (header == null) {
            return Duration.ofSeconds(1);
        }
        try {
            long millis = (long) Math.ceil(Double.parseDouble(header) * 1000);
            return Duration.ofMillis(Math.max(0, millis));
        } catch (NumberFormatException ignored) {
            return Duration.ofSeconds(1);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Discord 재시도 대기 중 중단되었습니다.", e);
        }
    }

    private static IllegalStateException failure(RestClientResponseException e) {
        String reason = switch (HttpStatus.resolve(e.getStatusCode().value())) {
            case UNAUTHORIZED, FORBIDDEN -> "Webhook 권한이 없습니다";
            case NOT_FOUND -> "Webhook을 찾을 수 없습니다(삭제되었거나 URL이 잘못됨)";
            case TOO_MANY_REQUESTS -> "rate limit에 걸렸습니다";
            case BAD_REQUEST -> "요청 형식이 잘못되었습니다";
            case null, default -> "알 수 없는 오류";
        };
        return new IllegalStateException(
                "Discord Webhook 오류 (HTTP %d): %s. 응답: %s"
                        .formatted(e.getStatusCode().value(), reason, e.getResponseBodyAsString()), e);
    }

    /** allowed_mentions.parse를 비워 @everyone, @here, 사용자/역할 멘션 알림을 모두 막는다. */
    record WebhookRequest(String content, @JsonProperty("allowed_mentions") AllowedMentions allowedMentions) {
    }

    record AllowedMentions(List<String> parse) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WebhookMessageResponse(String id) {
    }
}
