package com.moa.backend.googleauth.domain;

import java.time.Duration;
import java.time.Instant;

/** Google API 호출에 쓰는 단기 access token. */
public record GoogleAccessToken(String value, Instant expiresAt) {

    /** 만료 직전 토큰으로 요청이 나가지 않도록 이만큼 일찍 만료된 것으로 본다. */
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(60);

    public GoogleAccessToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("access token이 비어 있습니다.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("access token 만료 시각이 없습니다.");
        }
    }

    public boolean isUsableAt(Instant now) {
        return now.plus(EXPIRY_MARGIN).isBefore(expiresAt);
    }
}
