package com.moa.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI: http://localhost:8080/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI moaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MOA Backend API")
                .version("0.0.1")
                .description("MCP 도구(CSV, Discord, Google 폼)를 HTTP로 직접 호출해 보는 테스트 API와 "
                        + "Google 계정 연결(OAuth) API. MCP 엔드포인트 자체는 /mcp (Streamable HTTP)."));
    }
}
