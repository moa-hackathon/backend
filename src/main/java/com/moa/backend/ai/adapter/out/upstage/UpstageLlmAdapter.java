package com.moa.backend.ai.adapter.out.upstage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.backend.ai.application.port.out.LlmPort;
import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.ai.domain.AiPrompt;
import com.moa.backend.ai.domain.AiProvider;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Upstage Solar Chat Completions 어댑터 (OpenAI 호환 API).
 * POST {baseUrl}/chat/completions, Authorization: Bearer {apiKey}
 */
@Component
public class UpstageLlmAdapter implements LlmPort {

    private static final int MAX_ERROR_BODY = 500;

    private final RestClient restClient;
    private final UpstageProperties properties;

    public UpstageLlmAdapter(RestClient.Builder restClientBuilder, UpstageProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        this.restClient = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .baseUrl(properties.baseUrl())
                .build();
        this.properties = properties;
    }

    @Override
    public AiProvider provider() {
        return AiProvider.UPSTAGE;
    }

    @Override
    public AiCompletion complete(AiPrompt prompt) {
        String apiKey = properties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Upstage API 키가 설정되지 않았습니다. 환경변수 UPSTAGE_API_KEY를 지정하세요.");
        }

        List<ChatMessage> messages = new ArrayList<>();
        if (prompt.hasSystemPrompt()) {
            messages.add(new ChatMessage("system", prompt.systemPrompt()));
        }
        messages.add(new ChatMessage("user", prompt.userPrompt()));

        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .headers(h -> h.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChatRequest(properties.model(), messages))
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientResponseException e) {
            throw failure(e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Upstage API 호출에 실패했습니다(네트워크/타임아웃): " + e.getClass().getSimpleName(), e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null) {
            throw new IllegalStateException("Upstage 응답에 답변이 없습니다.");
        }
        Choice choice = response.choices().get(0);
        Usage usage = response.usage();
        return new AiCompletion(
                choice.message().content(),
                AiProvider.UPSTAGE,
                response.model(),
                "length".equals(choice.finishReason()),
                usage == null ? null : usage.promptTokens(),
                usage == null ? null : usage.completionTokens());
    }

    private static IllegalStateException failure(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body.length() > MAX_ERROR_BODY) {
            body = body.substring(0, MAX_ERROR_BODY) + "...";
        }
        String hint = switch (HttpStatus.resolve(e.getStatusCode().value())) {
            case UNAUTHORIZED, FORBIDDEN -> "API 키가 올바르지 않거나 권한이 없습니다";
            case NOT_FOUND -> "모델 이름 또는 주소가 잘못되었습니다(UPSTAGE_MODEL 확인)";
            case TOO_MANY_REQUESTS -> "요청 한도를 넘었거나 크레딧이 부족합니다";
            case BAD_REQUEST -> "요청 내용이 잘못되었습니다(입력이 너무 길 수 있음)";
            case null, default -> "Upstage API 오류";
        };
        return new IllegalStateException(
                "Upstage API 오류 (HTTP %d): %s. 응답: %s".formatted(e.getStatusCode().value(), hint, body), e);
    }

    record ChatRequest(String model, List<ChatMessage> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(String model, List<Choice> choices, Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message, @JsonProperty("finish_reason") String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") Integer promptTokens,
                 @JsonProperty("completion_tokens") Integer completionTokens) {
    }
}
