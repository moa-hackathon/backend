package com.moa.backend.ai.application.port.out;

import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.ai.domain.AiPrompt;
import com.moa.backend.ai.domain.AiProvider;

/**
 * 텍스트를 생성하는 LLM에 대한 출력 포트. 제공자마다 어댑터 하나씩 구현한다.
 * 새 제공자를 붙일 때는 {@link AiProvider}에 값을 추가하고 이 포트를 구현한 어댑터를 만든다.
 */
public interface LlmPort {

    /** 이 어댑터가 담당하는 제공자 */
    AiProvider provider();

    AiCompletion complete(AiPrompt prompt);
}
