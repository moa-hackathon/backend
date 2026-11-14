package com.moa.backend.event.dto;

import com.moa.backend.event.domain.EventDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** "이대로 확인": 행사 요약, 확정한 공지 본문, 추가 질문으로 행사를 만든다. */
public record EventCreateRequest(
		@Schema(description = "행사명", example = "모아 네트워킹 행사") String title,
		@Schema(description = "행사 일시", example = "2026-11-21T19:00:00") LocalDateTime eventAt,
		@Schema(description = "장소", example = "공학관 302호") String location,
		@Schema(description = "정원 (1 이상)", example = "40") Integer capacity,
		@Schema(description = "최소 필수 인원 (0 이상 정원 이하, 생략하면 0)", example = "20") Integer minCapacity,
		@Schema(description = "신청 마감 (행사 일시 이전)", example = "2026-11-19T23:59:00") LocalDateTime applyDeadline,
		@Schema(description = "신청받을 때 추가로 물어볼 질문", example = "[\"뒤풀이 참석 여부\"]") List<String> extraQuestions,
		@Schema(description = "기타 안내 (선택)", example = "피자 나와요") String description,
		@Schema(description = "확정한 공지 본문. 신청 링크 자리는 [구글폼 링크]") String noticeText
) {

	public EventDetails toDetails() {
		return EventDetails.of(title, eventAt, location, capacity, minCapacity, applyDeadline, extraQuestions,
				description);
	}
}
