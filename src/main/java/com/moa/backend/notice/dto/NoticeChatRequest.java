package com.moa.backend.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "공지 입력 채팅 요청. 서버는 대화를 저장하지 않으므로 지금까지의 대화를 모두 보낸다")
public record NoticeChatRequest(
		@Schema(description = "지금까지의 대화. 마지막은 운영진(user) 메시지여야 한다")
		@NotEmpty
		@Size(max = NoticeChatRequest.MAX_MESSAGES)
		List<@Valid NoticeChatMessage> messages,

		@Schema(description = "직전 응답으로 받은 행사 요약. 첫 요청이면 생략")
		NoticeChatSummary summary,

		@Schema(description = "추천 칩에서 고른 추가 질문. 고르지 않았으면 생략", example = "[\"뒤풀이 참석 여부\"]")
		@Size(max = NoticeChatSummary.MAX_EXTRA_QUESTIONS)
		List<String> selectedExtraQuestions) {

	public static final int MAX_MESSAGES = 40;
}
