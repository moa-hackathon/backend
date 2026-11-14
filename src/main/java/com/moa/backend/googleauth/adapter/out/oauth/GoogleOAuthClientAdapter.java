package com.moa.backend.googleauth.adapter.out.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.backend.googleauth.application.port.out.GoogleOAuthClientPort;
import com.moa.backend.googleauth.domain.GoogleAccessToken;
import com.moa.backend.googleauth.domain.GoogleReauthorizationRequiredException;
import com.moa.backend.googleauth.domain.GoogleTokenGrant;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/** Google OAuth 2.0 (웹 서버 앱 흐름) 어댑터. 오류 메시지에 토큰·비밀번호를 넣지 않는다. */
@Component
public class GoogleOAuthClientAdapter implements GoogleOAuthClientPort {

    static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    private final RestClient restClient;
    private final GoogleOAuthProperties properties;
    private final Clock clock = Clock.systemUTC();

    public GoogleOAuthClientAdapter(RestClient.Builder restClientBuilder, GoogleOAuthProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public String authorizationUrl(String state) {
        requireConfigured();
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_ENDPOINT)
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.scope())
                .queryParam("access_type", "offline") // refresh token 발급
                .queryParam("prompt", "consent")      // 다시 연결해도 refresh token을 새로 받도록
                .queryParam("state", state)
                .encode()
                .toUriString();
    }

    @Override
    public GoogleTokenGrant exchange(String code) {
        requireConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri());
        form.add("grant_type", "authorization_code");

        TokenResponse response = postToken(form, "인가 코드 교환");
        return new GoogleTokenGrant(toAccessToken(response), response.refreshToken(), response.scope());
    }

    @Override
    public GoogleAccessToken refresh(String refreshToken) {
        requireConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("grant_type", "refresh_token");

        return toAccessToken(postToken(form, "access token 갱신"));
    }

    private TokenResponse postToken(MultiValueMap<String, String> form, String action) {
        TokenResponse response;
        try {
            response = restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            if (body.contains("invalid_grant")) {
                throw new GoogleReauthorizationRequiredException(
                        "Google %s 실패: 토큰이 만료되었거나 철회되었습니다.".formatted(action), e);
            }
            throw new IllegalStateException(
                    "Google %s 실패 (HTTP %d): %s".formatted(action, e.getStatusCode().value(), body), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Google %s 요청에 실패했습니다: %s".formatted(action, e.getClass().getSimpleName()), e);
        }
        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Google %s 응답에 access token이 없습니다.".formatted(action));
        }
        return response;
    }

    private GoogleAccessToken toAccessToken(TokenResponse response) {
        long expiresIn = response.expiresIn() == null ? 0 : response.expiresIn();
        return new GoogleAccessToken(response.accessToken(), Instant.now(clock).plusSeconds(expiresIn));
    }

    private void requireConfigured() {
        if (isBlank(properties.clientId()) || isBlank(properties.clientSecret())) {
            throw new IllegalStateException(
                    "Google OAuth 클라이언트가 설정되지 않았습니다. 환경변수 GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET을 지정하세요.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("scope") String scope) {
    }
}
