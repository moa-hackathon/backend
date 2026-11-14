package com.moa.backend.notice.domain;

import java.util.regex.Pattern;

/**
 * 공지 본문의 신청 링크 자리 {@code [구글폼 링크]}. 공지는 이 자리를 남긴 채 저장하고, 발송 화면에서 실제 링크로 바꾼다.
 */
public final class FormLinkPlaceholder {

	public static final String VALUE = "[구글폼 링크]";

	/** 공지 끝에 붙이는 신청 링크 줄. */
	public static final String LINE = "신청 링크: " + VALUE;

	/** 모델이 지어낸 링크. 실제 링크는 아직 없으므로 모두 자리 표시로 바꾼다. */
	private static final Pattern URL = Pattern.compile("https?://\\S+");

	/** {@code [구글 폼 링크]}, {@code {구글폼링크}}처럼 살짝 다르게 쓴 자리 표시. */
	private static final Pattern VARIANT = Pattern.compile("[\\[{(<]\\s*구글\\s*폼\\s*(신청\\s*)?링크\\s*[\\]})>]");

	private FormLinkPlaceholder() {
	}

	/**
	 * 지어낸 링크와 변형된 자리 표시를 {@code [구글폼 링크]}로 맞추고, 자리 표시가 없으면 마지막 줄에 신청 링크 줄을 붙인다.
	 */
	public static String ensure(String text) {
		String normalized = VARIANT.matcher(text).replaceAll(VALUE);
		normalized = URL.matcher(normalized).replaceAll(VALUE).strip();
		if (normalized.contains(VALUE)) {
			return normalized;
		}
		return normalized.isEmpty() ? LINE : normalized + "\n" + LINE;
	}
}
