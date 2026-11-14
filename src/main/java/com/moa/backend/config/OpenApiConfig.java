package com.moa.backend.config;

import com.moa.backend.common.auth.CurrentClubArgumentResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String INVITE_CODE_SCHEME = "inviteCode";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("MOA API")
						.version("v1"))
				.components(new Components()
						.addSecuritySchemes(INVITE_CODE_SCHEME, new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.HEADER)
								.name(CurrentClubArgumentResolver.INVITE_CODE_HEADER)))
				.addSecurityItem(new SecurityRequirement().addList(INVITE_CODE_SCHEME));
	}
}
