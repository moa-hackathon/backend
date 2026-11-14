package com.moa.backend.googleauth.application.port.in;

public interface CompleteGoogleAuthorizationUseCase {

    /** Google이 돌려준 인가 코드를 토큰으로 교환해 저장한다. */
    void complete(String code, String state);
}
