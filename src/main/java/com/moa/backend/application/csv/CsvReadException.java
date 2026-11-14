package com.moa.backend.application.csv;

import lombok.Getter;

/** 업로드된 파일 전체를 CSV로 읽을 수 없을 때 던진다. 행 단위 실패는 예외 대신 {@link CsvRowError}로 모은다. */
@Getter
public class CsvReadException extends RuntimeException {

	private final Reason reason;

	public CsvReadException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public enum Reason {
		/** 비어 있는 파일 */
		EMPTY,
		/** 텍스트 CSV가 아니거나 형식이 깨진 파일 */
		UNREADABLE,
		/** 필수 열(이름, 타임스탬프)을 찾지 못함 */
		COLUMN_MISSING
	}
}
