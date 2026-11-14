package com.moa.backend.ai.adapter.out.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.moa.backend.ai.application.port.out.LlmPort;
import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.ai.domain.AiPrompt;
import com.moa.backend.ai.domain.AiProvider;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Google Gemini generateContent 어댑터.
 * POST {baseUrl}/models/{model}:generateContent, x-goog-api-key: {apiKey}
 */
@Component
public class GeminiLlmAdapter implements LlmPort {

    private static final int MAX_ERROR_BODY = 500;

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiLlmAdapter(RestClient.Builder restClientBuilder, GeminiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
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
        return AiProvider.GEMINI;
    }

    @Override
    public AiCompletion complete(AiPrompt prompt) {
        String apiKey = properties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API 키가 설정되지 않았습니다. 환경변수 GEMINI_API_KEY를 지정하세요.");
        }

        GenerateRequest request = new GenerateRequest(
                prompt.hasSystemPrompt() ? new Content(null, List.of(new Part(prompt.systemPrompt(), null))) : null,
                List.of(new Content("user", List.of(new Part(prompt.userPrompt(), null)))));

        GenerateResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GenerateResponse.class);
        } catch (RestClientResponseException e) {
            throw failure(e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Gemini API 호출에 실패했습니다(네트워크/타임아웃): " + e.getClass().getSimpleName(), e);
        }

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            String reason = response != null && response.promptFeedback() != null
                    ? response.promptFeedback().blockReason() : null;
            throw new IllegalStateException(reason != null
                    ? "Gemini가 프롬프트를 차단했습니다: " + reason
                    : "Gemini 응답에 답변이 없습니다.");
        }

        Candidate candidate = response.candidates().get(0);
        String text = candidate.content() == null || candidate.content().parts() == null ? ""
                : candidate.content().parts().stream()
                        .filter(part -> !Boolean.TRUE.equals(part.thought())) // 사고 과정 요약은 답변에서 제외
                        .map(Part::text)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining());
        if (text.isEmpty()) {
            throw new IllegalStateException("Gemini가 답변을 만들지 못했습니다. finishReason: " + candidate.finishReason());
        }

        UsageMetadata usage = response.usageMetadata();
        return new AiCompletion(
                text,
                AiProvider.GEMINI,
                response.modelVersion() != null ? response.modelVersion() : properties.model(),
                "MAX_TOKENS".equals(candidate.finishReason()),
                usage == null ? null : usage.promptTokenCount(),
                usage == null ? null : usage.candidatesTokenCount());
    }

    private static IllegalStateException failure(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body.length() > MAX_ERROR_BODY) {
            body = body.substring(0, MAX_ERROR_BODY) + "...";
        }
        String hint = switch (HttpStatus.resolve(e.getStatusCode().value())) {
            case UNAUTHORIZED, FORBIDDEN -> "API 키가 올바르지 않거나 권한이 없습니다";
            case NOT_FOUND -> "모델 이름이 잘못되었거나 종료된 모델입니다(GEMINI_MODEL 확인)";
            case TOO_MANY_REQUESTS -> "요청 한도를 넘었습니다";
            case BAD_REQUEST -> "요청 내용이 잘못되었거나 API 키가 유효하지 않습니다";
            case null, default -> "Gemini API 오류";
        };
        return new IllegalStateException(
                "Gemini API 오류 (HTTP %d): %s. 응답: %s".formatted(e.getStatusCode().value(), hint, body), e);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerateRequest(Content systemInstruction, List<Content> contents) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(String role, List<Part> parts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text, Boolean thought) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates, UsageMetadata usageMetadata, String modelVersion,
                            PromptFeedback promptFeedback) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UsageMetadata(Integer promptTokenCount, Integer candidatesTokenCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PromptFeedback(String blockReason) {
    }
}
