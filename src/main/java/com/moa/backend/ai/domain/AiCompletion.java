package com.moa.backend.ai.domain;

/**
 * LLM 응답.
 *
 * @param text         모델이 생성한 답변
 * @param provider     응답한 제공자
 * @param model        실제로 응답한 모델 (버전 포함)
 * @param truncated    출력 길이 한도에 걸려 답변이 중간에 끊겼는지
 * @param inputTokens  입력 토큰 수 (제공되지 않으면 null)
 * @param outputTokens 출력 토큰 수 (제공되지 않으면 null)
 */
public record AiCompletion(
        String text,
        AiProvider provider,
        String model,
        boolean truncated,
        Integer inputTokens,
        Integer outputTokens) {
}
