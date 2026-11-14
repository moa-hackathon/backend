package com.moa.backend.ai.application.port.in;

import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.ai.domain.AiProvider;

public interface GenerateTextUseCase {

    /**
     * @param provider     쓸 제공자. null이면 기본 제공자(moa.ai.default-provider)
     * @param systemPrompt 모델의 역할·규칙 (null 가능)
     * @param userPrompt   사용자 질문·지시
     */
    AiCompletion generate(AiProvider provider, String systemPrompt, String userPrompt);
}
