package com.moa.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.stream.IntStream;

public record FormQuestionsResponse(
		@Schema(description = "추천 문항. 이름 / 학번 / 연락처(필수) 다음에 행사의 추가 질문(선택)")
		List<FormQuestionResponse> questions,
		@Schema(description = "'문항 복사하기' 버튼이 복사할 글",
				example = "1. 이름 (필수)\n2. 학번 (필수)\n3. 연락처 (필수)\n4. 뒤풀이 참석 여부 (선택)")
		String copyText
) {

	public static FormQuestionsResponse of(List<FormQuestionResponse> questions) {
		String copyText = String.join("\n", IntStream.range(0, questions.size())
				.mapToObj(i -> "%d. %s (%s)".formatted(i + 1, questions.get(i).title(),
						questions.get(i).required() ? "필수" : "선택"))
				.toList());
		return new FormQuestionsResponse(List.copyOf(questions), copyText);
	}
}
