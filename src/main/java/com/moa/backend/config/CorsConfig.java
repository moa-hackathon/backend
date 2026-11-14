package com.moa.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프론트(Vercel 등)에서 오는 브라우저 요청을 허용한다.
 * 허용 origin은 moa.cors.allowed-origins(환경변수 CORS_ALLOWED_ORIGINS, 쉼표 구분)로 바꾼다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;

	public CorsConfig(@Value("${moa.cors.allowed-origins}") String[] allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				// 엑셀 다운로드 파일명을 프론트에서 읽을 수 있게 노출
				.exposedHeaders(HttpHeaders.CONTENT_DISPOSITION)
				.maxAge(3600);
	}
}
