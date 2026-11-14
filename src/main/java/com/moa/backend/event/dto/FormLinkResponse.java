package com.moa.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FormLinkResponse(
		@Schema(description = "행사 ID", example = "10")
		Long eventId,
		@Schema(description = "저장된 구글폼 링크 (붙여넣은 그대로)", example = "https://forms.gle/AbCdEf123")
		String formUrl,
		@Schema(description = "폼 제목. 읽지 못하면 null", example = "모아 네트워킹 행사 신청")
		String formTitle,
		@Schema(description = "폼의 응답 항목 (폼에 있는 순서)")
		List<FormQuestionResponse> questions,
		@Schema(description = "화면 말풍선에 쓸 확인 문구",
				example = "링크를 확인했어요. 응답 항목: 이름, 학번, 연락처, 뒤풀이 참석 여부")
		String message
) {

	public static FormLinkResponse of(Long eventId, String formUrl, String formTitle,
			List<FormQuestionResponse> questions) {
		String message = questions.isEmpty()
				? "링크를 확인했어요. 폼에 응답 항목이 아직 없어요."
				: "링크를 확인했어요. 응답 항목: " + String.join(", ",
						questions.stream().map(FormQuestionResponse::title).toList());
		return new FormLinkResponse(eventId, formUrl, formTitle, List.copyOf(questions), message);
	}
}
