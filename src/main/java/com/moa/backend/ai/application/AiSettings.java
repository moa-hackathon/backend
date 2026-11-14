package com.moa.backend.ai.application;

import com.moa.backend.ai.domain.AiProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param defaultProvider 요청에 제공자를 지정하지 않았을 때 쓸 제공자 (upstage / gemini) */
@ConfigurationProperties(prefix = "moa.ai")
public record AiSettings(@DefaultValue("UPSTAGE") AiProvider defaultProvider) {
}
