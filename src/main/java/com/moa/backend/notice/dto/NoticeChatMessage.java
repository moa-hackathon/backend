package com.moa.backend.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "채팅 메시지 하나")
public record NoticeChatMessage(
		@Schema(description = "user(운영진) 또는 assistant(AI)", example = "user")
		@NotNull
		NoticeChatRole role,

		@Schema(description = "메시지 내용", example = "다음 주 금요일 저녁 7시에 네트워킹 행사 해요. 20명은 넘어야 진행해요.")
		@NotBlank
		@Size(max = NoticeChatMessage.MAX_CONTENT_LENGTH)
		String content) {

	public static final int MAX_CONTENT_LENGTH = 2000;
}
