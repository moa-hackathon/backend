package com.moa.backend.googleauth.application.port.in;

public interface StartGoogleAuthorizationUseCase {

    /** @return 사용자를 보낼 Google 동의 화면 URL (CSRF 방지용 state 포함) */
    String start();
}
