package com.moa.backend.ai.application;

import com.moa.backend.ai.application.port.in.GenerateTextUseCase;
import com.moa.backend.ai.application.port.out.LlmPort;
import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.ai.domain.AiPrompt;
import com.moa.backend.ai.domain.AiProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiService implements GenerateTextUseCase {

    private final Map<AiProvider, LlmPort> llmPorts = new EnumMap<>(AiProvider.class);
    private final AiProvider defaultProvider;

    public AiService(List<LlmPort> llmPorts, AiSettings settings) {
        for (LlmPort port : llmPorts) {
            LlmPort previous = this.llmPorts.put(port.provider(), port);
            if (previous != null) {
                throw new IllegalStateException("AI 제공자 %s의 어댑터가 두 개 이상 있습니다.".formatted(port.provider()));
            }
        }
        this.defaultProvider = settings.defaultProvider();
    }

    @Override
    public AiCompletion generate(AiProvider provider, String systemPrompt, String userPrompt) {
        AiProvider target = provider == null ? defaultProvider : provider;
        LlmPort port = llmPorts.get(target);
        if (port == null) {
            throw new IllegalArgumentException("지원하지 않는 AI 제공자입니다: " + target);
        }
        return port.complete(new AiPrompt(systemPrompt, userPrompt));
    }
}
