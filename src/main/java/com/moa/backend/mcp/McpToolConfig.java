package com.moa.backend.mcp;

import java.util.List;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 모든 {@link McpToolSet} 빈의 @Tool 메서드를 MCP 서버 도구로 등록한다. */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(List<McpToolSet> toolSets) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(toolSets.toArray())
                .build();
    }
}
