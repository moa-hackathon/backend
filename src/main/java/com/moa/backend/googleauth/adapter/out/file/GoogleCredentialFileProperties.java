package com.moa.backend.googleauth.adapter.out.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param file refresh token을 저장할 파일 경로. 저장소(git)에 올라가지 않는 곳이어야 한다. */
@ConfigurationProperties(prefix = "moa.google.credential")
public record GoogleCredentialFileProperties(@DefaultValue("./data/google/credential.json") String file) {
}
