package com.moa.backend.notice.dto;

import com.moa.backend.dashboard.domain.NextAction;
import io.swagger.v3.oas.annotations.media.Schema;

public record FollowUpNoticeDraftResponse(
		@Schema(description = "후속 공지 유형", example = "CLOSING_SOON")
		NextAction.Type type,
		@Schema(description = "후속 공지 초안. 신청 링크 자리는 [구글폼 링크]로 둡니다.")
		String noticeText,
		@Schema(description = "AI가 쓴 초안인지(AI), 기본 양식인지(TEMPLATE)")
		NoticeDraftSource source
) {
}
