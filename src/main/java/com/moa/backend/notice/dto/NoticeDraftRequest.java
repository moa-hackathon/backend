package com.moa.backend.notice.dto;

import com.moa.backend.event.domain.EventDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 공지 초안 생성 요청. 처음 만들 때는 행사 요약만, 수정 요청일 때는 이전 초안과 수정 요청 문구도 함께 보낸다.
 */
public record NoticeDraftRequest(
		@Schema(description = "행사명", example = "모아 네트워킹 행사") String title,
		@Schema(description = "행사 일시", example = "2026-11-21T19:00:00") LocalDateTime eventAt,
		@Schema(description = "장소", example = "공학관 302호") String location,
		@Schema(description = "정원 (1 이상)", example = "40") Integer capacity,
		@Schema(description = "최소 필수 인원 (0 이상 정원 이하, 생략하면 0)", example = "20") Integer minCapacity,
		@Schema(description = "신청 마감 (행사 일시 이전)", example = "2026-11-19T23:59:00") LocalDateTime applyDeadline,
		@Schema(description = "신청받을 때 추가로 물어볼 질문", example = "[\"뒤풀이 참석 여부\"]") List<String> extraQuestions,
		@Schema(description = "공지에 넣을 기타 안내 (선택)", example = "피자 나와요") String description,
		@Schema(description = "수정 요청일 때 이전 초안") String previousDraft,
		@Schema(description = "수정 요청 문구. 있으면 previousDraft를 이 요청대로 고친다", example = "좀 더 친근하게 써 줘")
		String revisionRequest
) {

	public EventDetails toDetails() {
		return EventDetails.of(title, eventAt, location, capacity, minCapacity, applyDeadline, extraQuestions,
				description);
	}

	public boolean isRevision() {
		return revisionRequest != null && !revisionRequest.isBlank();
	}
}
