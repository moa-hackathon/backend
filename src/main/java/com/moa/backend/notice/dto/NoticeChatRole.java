package com.moa.backend.notice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 공지 입력 채팅에서 메시지를 쓴 쪽. */
public enum NoticeChatRole {
	/** 운영진 */
	@JsonProperty("user")
	USER,
	/** AI */
	@JsonProperty("assistant")
	ASSISTANT
}
