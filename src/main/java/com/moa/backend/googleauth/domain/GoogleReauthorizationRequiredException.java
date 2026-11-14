package com.moa.backend.googleauth.domain;

/** 저장된 자격 증명이 없거나 만료·철회되어 사용자가 다시 Google 계정을 연결해야 할 때. */
public class GoogleReauthorizationRequiredException extends RuntimeException {

    public static final String GUIDE =
            "서버가 실행 중인 PC의 브라우저에서 /oauth/google/authorize 에 접속해 Google 계정을 연결하세요.";

    public GoogleReauthorizationRequiredException(String reason) {
        super(reason + " " + GUIDE);
    }

    public GoogleReauthorizationRequiredException(String reason, Throwable cause) {
        super(reason + " " + GUIDE, cause);
    }
}
