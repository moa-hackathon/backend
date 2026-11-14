package com.moa.backend.googleauth.adapter.in.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param allowRemote false(기본)면 /oauth/google/** 를 서버 PC(localhost)에서 온 요청만 받는다.
 *                    아무나 자기 계정을 연결해 토큰을 바꿔치기하는 것을 막는다.
 */
@ConfigurationProperties(prefix = "moa.google.oauth-web")
public record GoogleOAuthWebProperties(@DefaultValue("false") boolean allowRemote) {
}
