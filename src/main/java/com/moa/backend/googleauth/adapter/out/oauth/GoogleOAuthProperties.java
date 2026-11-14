package com.moa.backend.googleauth.adapter.out.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param clientId     GCP 콘솔 → OAuth 클라이언트(웹 애플리케이션)의 클라이언트 ID
 * @param clientSecret 같은 클라이언트의 보안 비밀번호. 환경변수로만 주입한다.
 * @param redirectUri  OAuth 클라이언트에 "승인된 리디렉션 URI"로 등록한 값과 정확히 같아야 한다.
 * @param scope        drive.file: 이 앱이 만든 파일(폼)만 다룰 수 있는 비민감 스코프
 */
@ConfigurationProperties(prefix = "moa.google.oauth")
public record GoogleOAuthProperties(
        String clientId,
        String clientSecret,
        @DefaultValue("http://localhost:8080/oauth/google/callback") String redirectUri,
        @DefaultValue("https://www.googleapis.com/auth/drive.file") String scope) {
}
