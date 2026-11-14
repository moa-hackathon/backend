package com.moa.backend.googleauth.application;

import com.moa.backend.googleauth.application.port.in.CompleteGoogleAuthorizationUseCase;
import com.moa.backend.googleauth.application.port.in.GetGoogleAccessTokenUseCase;
import com.moa.backend.googleauth.application.port.in.StartGoogleAuthorizationUseCase;
import com.moa.backend.googleauth.application.port.out.GoogleCredentialStorePort;
import com.moa.backend.googleauth.application.port.out.GoogleOAuthClientPort;
import com.moa.backend.googleauth.domain.GoogleAccessToken;
import com.moa.backend.googleauth.domain.GoogleCredential;
import com.moa.backend.googleauth.domain.GoogleReauthorizationRequiredException;
import com.moa.backend.googleauth.domain.GoogleTokenGrant;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService
        implements StartGoogleAuthorizationUseCase, CompleteGoogleAuthorizationUseCase, GetGoogleAccessTokenUseCase {

    /** 동의 화면을 띄운 뒤 이 시간 안에 돌아와야 한다. */
    static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final GoogleOAuthClientPort oauthClient;
    private final GoogleCredentialStorePort credentialStore;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();

    private GoogleAccessToken cachedToken;

    @Autowired
    public GoogleAuthService(GoogleOAuthClientPort oauthClient, GoogleCredentialStorePort credentialStore) {
        this(oauthClient, credentialStore, Clock.systemUTC());
    }

    GoogleAuthService(GoogleOAuthClientPort oauthClient, GoogleCredentialStorePort credentialStore, Clock clock) {
        this.oauthClient = oauthClient;
        this.credentialStore = credentialStore;
        this.clock = clock;
    }

    @Override
    public String start() {
        Instant now = clock.instant();
        pendingStates.values().removeIf(expiresAt -> !now.isBefore(expiresAt));

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pendingStates.put(state, now.plus(STATE_TTL));
        return oauthClient.authorizationUrl(state);
    }

    @Override
    public void complete(String code, String state) {
        Instant expiresAt = state == null ? null : pendingStates.remove(state);
        if (expiresAt == null || !clock.instant().isBefore(expiresAt)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 요청입니다. /oauth/google/authorize 부터 다시 시작하세요.");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Google 인가 코드가 없습니다.");
        }

        GoogleTokenGrant grant = oauthClient.exchange(code);
        if (grant.refreshToken() == null || grant.refreshToken().isBlank()) {
            throw new IllegalStateException(
                    "Google이 refresh token을 주지 않았습니다. https://myaccount.google.com/permissions 에서 "
                            + "이 앱의 접근 권한을 삭제한 뒤 다시 연결하세요.");
        }
        credentialStore.save(new GoogleCredential(grant.refreshToken(), grant.scope(), clock.instant()));
        synchronized (this) {
            cachedToken = grant.accessToken();
        }
    }

    @Override
    public synchronized String accessToken() {
        if (cachedToken != null && cachedToken.isUsableAt(clock.instant())) {
            return cachedToken.value();
        }
        GoogleCredential credential = credentialStore.load()
                .orElseThrow(() -> new GoogleReauthorizationRequiredException("연결된 Google 계정이 없습니다."));
        try {
            cachedToken = oauthClient.refresh(credential.refreshToken());
        } catch (RuntimeException e) {
            cachedToken = null;
            throw e;
        }
        return cachedToken.value();
    }
}
