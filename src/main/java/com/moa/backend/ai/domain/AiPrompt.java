package com.moa.backend.ai.domain;

/**
 * LLM에 보낼 요청.
 *
 * @param systemPrompt 모델의 역할·규칙 (선택)
 * @param userPrompt   사용자 질문·지시 (필수)
 */
public record AiPrompt(String systemPrompt, String userPrompt) {

    public AiPrompt {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("프롬프트가 비어 있습니다.");
        }
        if (systemPrompt != null && systemPrompt.isBlank()) {
            systemPrompt = null;
        }
    }

    public boolean hasSystemPrompt() {
        return systemPrompt != null;
    }
}
