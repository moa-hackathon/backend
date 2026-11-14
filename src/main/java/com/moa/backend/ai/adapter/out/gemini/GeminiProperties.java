package com.moa.backend.ai.adapter.out.gemini;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param apiKey  Google AI Studio(aistudio.google.com)에서 발급한 API 키. 환경변수로만 주입한다.
 * @param baseUrl Gemini API 기준 주소
 * @param model   모델 코드. gemini-3.6-flash(범용) / gemini-3.5-flash-lite(빠르고 저렴)
 * @param timeout 응답 대기 시간
 */
@ConfigurationProperties(prefix = "moa.ai.gemini")
public record GeminiProperties(
        String apiKey,
        @DefaultValue("https://generativelanguage.googleapis.com/v1beta") String baseUrl,
        @DefaultValue("gemini-3.6-flash") String model,
        @DefaultValue("60s") Duration timeout) {
}
