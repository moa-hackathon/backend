package com.moa.backend.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "발송할 최종 공지")
public record FinalNoticeResponse(
		@Schema(description = "행사 ID", example = "10")
		Long eventId,
		@Schema(description = "최종 공지 본문. 구글폼 링크가 아직 없으면 [구글폼 링크] 자리 표시가 그대로 남는다.")
		String noticeText,
		@Schema(description = "연결된 구글폼 링크. 없으면 null", example = "https://forms.gle/abc123")
		String formUrl,
		@Schema(description = "구글폼 링크가 아직 연결되지 않았는지. true면 발송할 수 없다.")
		boolean formLinkMissing,
		@Schema(description = "동아리에 디스코드 웹훅이 연결되어 있는지. false면 발송할 수 없다.")
		boolean discordConnected,
		@Schema(description = "마지막으로 발송한 시각. 아직 보내지 않았으면 null", example = "2026-11-14T13:00:00")
		LocalDateTime sentAt
) {
}
