package com.moa.backend.application.csv;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** CSV 원본 행의 필드를 구분자로 이어 SHA-256(소문자 hex 64자)으로 만든다. */
public final class RowHasher {

	/** 응답 본문에 나오지 않는 ASCII Unit Separator. 필드 경계가 바뀌면 해시도 달라지게 한다. */
	private static final String SEPARATOR = "\u001F";

	private RowHasher() {
	}

	public static String hash(List<String> fields) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(String.join(SEPARATOR, fields).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256을 지원하지 않는 JVM입니다.", e);
		}
	}
}
