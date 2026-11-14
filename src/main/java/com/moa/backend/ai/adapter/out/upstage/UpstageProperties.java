package com.moa.backend.ai.adapter.out.upstage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param apiKey  Upstage 콘솔(console.upstage.ai)에서 발급한 API 키. 환경변수로만 주입한다.
 * @param baseUrl OpenAI 호환 엔드포인트 기준 주소
 * @param model   모델 별칭. solar-pro4(고성능) / solar-mini4(빠르고 저렴)
 * @param timeout 응답 대기 시간. 긴 답변은 오래 걸릴 수 있다.
 */
@ConfigurationProperties(prefix = "moa.ai.upstage")
public record UpstageProperties(
        String apiKey,
        @DefaultValue("https://api.upstage.ai/v1") String baseUrl,
        @DefaultValue("solar-pro4") String model,
        @DefaultValue("60s") Duration timeout) {
}
