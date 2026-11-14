package com.moa.backend.googleauth.application.port.out;

import com.moa.backend.googleauth.domain.GoogleCredential;
import java.util.Optional;

/** refresh token을 보관하는 곳(파일, DB 등)에 대한 출력 포트. */
public interface GoogleCredentialStorePort {

    Optional<GoogleCredential> load();

    void save(GoogleCredential credential);
}
