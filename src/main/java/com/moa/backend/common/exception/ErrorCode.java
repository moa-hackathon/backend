package com.moa.backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVITE_CODE_REQUIRED(HttpStatus.UNAUTHORIZED, "초대 코드가 필요합니다."),
	INVITE_CODE_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 초대 코드입니다."),
	EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다."),
	CSV_EMPTY(HttpStatus.BAD_REQUEST, "빈 파일입니다."),
	CSV_UNREADABLE(HttpStatus.BAD_REQUEST, "CSV 파일을 읽을 수 없습니다."),
	CSV_COLUMN_MISSING(HttpStatus.BAD_REQUEST, "CSV에서 필요한 열을 찾을 수 없습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;
}
