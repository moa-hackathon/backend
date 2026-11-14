package com.moa.backend.dashboard.export;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 내려줄 엑셀 파일.
 *
 * @param fileName 행사명이 들어간 파일명 (한글 가능)
 * @param fallbackFileName {@code filename*}를 모르는 클라이언트용 ASCII 파일명
 */
public record ExcelDownload(String fileName, String fallbackFileName, byte[] content) {

	public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	/** 파일 시스템에서 쓸 수 없는 문자와 제어 문자. */
	private static final String UNSAFE_CHARACTERS = "[\\\\/:*?\"<>|\\p{Cntrl}]";

	public static ExcelDownload forEvent(Long eventId, String eventTitle, byte[] content) {
		String title = eventTitle == null ? "" : eventTitle.replaceAll(UNSAFE_CHARACTERS, "_").strip();
		String fileName = (title.isEmpty() ? "행사" : title) + "_신청자.xlsx";
		return new ExcelDownload(fileName, "event-" + eventId + "-applications.xlsx", content);
	}

	/** RFC 6266 / RFC 5987 형식. 한글 파일명은 {@code filename*}에 UTF-8 퍼센트 인코딩으로 담는다. */
	public String contentDisposition() {
		String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
		return "attachment; filename=\"" + fallbackFileName + "\"; filename*=UTF-8''" + encoded;
	}
}
