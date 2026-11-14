package com.moa.backend.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @param noticeText 공지 초안. 신청 링크 자리는 {@code [구글폼 링크]}로 남아 있다
 * @param source 초안을 만든 방식
 */
public record NoticeDraftResponse(
		@Schema(description = "공지 초안. 신청 링크 자리는 [구글폼 링크]") String noticeText,
		@Schema(description = "AI: AI가 작성, TEMPLATE: AI 응답을 못 받아 기본 양식으로 작성") NoticeDraftSource source
) {
}
