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
	DISCORD_WEBHOOK_INVALID(HttpStatus.BAD_REQUEST, "디스코드 웹훅 URL이 올바르지 않습니다."),
	DISCORD_NOT_CONNECTED(HttpStatus.CONFLICT, "디스코드가 연결되지 않았습니다."),
	DISCORD_SEND_FAILED(HttpStatus.BAD_GATEWAY, "디스코드 발송에 실패했습니다."),
	AI_RESPONSE_FAILED(HttpStatus.BAD_GATEWAY, "AI 응답을 받지 못했습니다."),
	FORM_LINK_INVALID(HttpStatus.BAD_REQUEST, "구글폼 링크를 확인할 수 없습니다."),
	NOTICE_NOT_READY(HttpStatus.CONFLICT, "공지가 아직 준비되지 않았습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다."),
	CSV_EMPTY(HttpStatus.BAD_REQUEST, "빈 파일입니다."),
	CSV_UNREADABLE(HttpStatus.BAD_REQUEST, "CSV 파일을 읽을 수 없습니다."),
	CSV_COLUMN_MISSING(HttpStatus.BAD_REQUEST, "CSV에서 필요한 열을 찾을 수 없습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;
}
