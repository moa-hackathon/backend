package com.moa.backend.googleauth.domain;

/**
 * 인가 코드를 교환해서 받은 결과.
 *
 * @param refreshToken Google은 동의 화면을 처음 거칠 때만 준다. 없을 수 있다(null).
 */
public record GoogleTokenGrant(GoogleAccessToken accessToken, String refreshToken, String scope) {
}
