package com.moa.backend.event.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 부원용 공유 링크 코드. 128비트 난수를 URL에 그대로 쓸 수 있는 22자로 만든다.
 * 추측할 수 없고 겹칠 확률도 무시할 만하다. 겹치더라도 {@code share_code} 유니크 제약이 막는다.
 */
@Component
public class ShareCodeGenerator {

	private static final int RANDOM_BYTES = 16;

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		byte[] bytes = new byte[RANDOM_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
