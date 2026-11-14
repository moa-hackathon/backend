package com.moa.backend.config;

import com.moa.backend.common.auth.CurrentClubArgumentResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI: http://localhost:8080/swagger-ui.html */
@Configuration
public class OpenApiConfig {

	private static final String INVITE_CODE_SCHEME = "inviteCode";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("MOA API")
						.version("v1")
						.description("MCP 도구(CSV, Discord, Google 폼·시트)를 HTTP로 직접 호출해 보는 테스트 API와 "
								+ "Google 계정 연결(OAuth) API. MCP 엔드포인트 자체는 /mcp (Streamable HTTP)."))
				.components(new Components()
						.addSecuritySchemes(INVITE_CODE_SCHEME, new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.HEADER)
								.name(CurrentClubArgumentResolver.INVITE_CODE_HEADER)))
				.addSecurityItem(new SecurityRequirement().addList(INVITE_CODE_SCHEME));
	}
}
