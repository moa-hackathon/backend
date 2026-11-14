package com.moa.backend.notice.dto;

import com.moa.backend.dashboard.domain.NextAction;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @param type 만들 후속 공지 유형. null이면 현재 진행 상태로 정한다.
 */
public record FollowUpNoticeDraftRequest(
		@Schema(description = "후속 공지 유형. 비우면 현재 진행 상태로 정합니다.", example = "CLOSING_SOON")
		NextAction.Type type
) {
}
