package com.moa.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 폼 문항 하나. 연결한 폼에서 읽은 문항과 추천 문항 모두 이 모양이다. */
public record FormQuestionResponse(
		@Schema(description = "문항 제목", example = "이름")
		String title,
		@Schema(description = "필수 응답 여부", example = "true")
		boolean required
) {
}
