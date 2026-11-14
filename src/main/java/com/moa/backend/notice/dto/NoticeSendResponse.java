package com.moa.backend.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "공지 발송 결과")
public record NoticeSendResponse(
		@Schema(description = "행사 ID", example = "10")
		Long eventId,
		@Schema(description = "발송한 채널", example = "DISCORD")
		String channel,
		@Schema(description = "발송 시각", example = "2026-11-14T13:00:00")
		LocalDateTime sentAt,
		@Schema(description = "보낸 디스코드 메시지 수. 2000자를 넘으면 여러 개로 나눠 보낸다.", example = "1")
		int messageCount
) {
}
