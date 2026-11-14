package com.moa.backend.googleauth.domain;

import java.time.Instant;

/**
 * 사용자가 OAuth로 허락해 준 Google 자격 증명. refresh token으로 access token을 다시 발급받는다.
 */
public record GoogleCredential(String refreshToken, String scope, Instant savedAt) {

    public GoogleCredential {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refresh token이 비어 있습니다.");
        }
    }
}
