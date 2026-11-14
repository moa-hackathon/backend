package com.moa.backend.common.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

	private final ErrorCode errorCode;

	public ApiException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	/** 기본 문구 대신 상황에 맞는 문구를 내려줄 때 쓴다. */
	public ApiException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
