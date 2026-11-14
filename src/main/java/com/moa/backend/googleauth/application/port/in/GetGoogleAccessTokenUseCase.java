package com.moa.backend.googleauth.application.port.in;

/** 다른 기능(Google 폼 등)이 Google API를 부를 때 쓰는 access token을 제공한다. */
public interface GetGoogleAccessTokenUseCase {

    /**
     * @throws com.moa.backend.googleauth.domain.GoogleReauthorizationRequiredException 계정 연결이 필요할 때
     */
    String accessToken();
}
