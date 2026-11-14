package com.moa.backend.googleauth.application.port.out;

import com.moa.backend.googleauth.domain.GoogleAccessToken;
import com.moa.backend.googleauth.domain.GoogleTokenGrant;

/** Google OAuth 서버와의 통신을 담당하는 출력 포트. */
public interface GoogleOAuthClientPort {

    String authorizationUrl(String state);

    GoogleTokenGrant exchange(String code);

    /**
     * @throws com.moa.backend.googleauth.domain.GoogleReauthorizationRequiredException refresh token이 만료·철회되었을 때
     */
    GoogleAccessToken refresh(String refreshToken);
}
