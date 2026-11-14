package com.moa.backend.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "공지 입력 채팅 응답")
public record NoticeChatResponse(
		@Schema(description = "AI 답변 (빠진 항목을 되묻는 문구 등)", example = "거의 다 됐어요. 아래 항목만 더 알려 주세요.")
		String reply,

		@Schema(description = "지금까지 정리된 행사 요약. 다음 요청 때 그대로 돌려보낸다")
		NoticeChatSummary summary,

		@Schema(description = "아직 비어 있는 필수 항목 (\"입력 필요\")", example = "[\"location\", \"capacity\", \"applyDeadline\"]")
		List<NoticeChatField> missingFields,

		@Schema(description = "추가 질문 추천 칩", example = "[\"뒤풀이 참석 여부\", \"알레르기\"]")
		List<String> suggestedExtraQuestions,

		@Schema(description = "필수 항목이 모두 채워졌는지. true면 공지 초안 API를 호출한다")
		boolean complete) {
}
