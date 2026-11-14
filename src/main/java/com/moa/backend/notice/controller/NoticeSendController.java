package com.moa.backend.notice.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.notice.dto.FinalNoticeResponse;
import com.moa.backend.notice.dto.NoticeSendResponse;
import com.moa.backend.notice.service.NoticeSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공지 발송", description = "운영진용 최종 공지 조회와 발송 API")
@RestController
@RequestMapping("/api/events/{eventId}/notice")
@RequiredArgsConstructor
public class NoticeSendController {

	private final NoticeSendService noticeSendService;

	@Operation(
			summary = "최종 공지 조회",
			description = "공지 본문의 [구글폼 링크] 자리를 연결된 구글폼 링크로 바꾼 최종 공지를 돌려줍니다. "
					+ "링크가 아직 없으면 자리 표시를 그대로 두고 formLinkMissing=true로 알려 줍니다. "
					+ "공지 본문이 없으면 409 NOTICE_NOT_READY, 다른 동아리의 행사면 404 EVENT_NOT_FOUND입니다.")
	@GetMapping
	public FinalNoticeResponse getFinalNotice(@CurrentClub Club club, @PathVariable Long eventId) {
		return noticeSendService.getFinalNotice(club, eventId);
	}

	@Operation(
			summary = "최종 공지 디스코드 발송",
			description = "최종 공지를 동아리에 연결된 디스코드 웹훅으로 보내고 발송 시각을 저장합니다. 다시 보내면 발송 시각이 갱신됩니다. "
					+ "공지 본문이나 구글폼 링크가 없으면 409 NOTICE_NOT_READY, 웹훅이 없으면 409 DISCORD_NOT_CONNECTED, "
					+ "저장된 웹훅 형식이 틀리면 400 DISCORD_WEBHOOK_INVALID, 공지가 너무 길면 400 INVALID_REQUEST, "
					+ "디스코드 호출이 실패하면 502 DISCORD_SEND_FAILED입니다.")
	@PostMapping("/send")
	public NoticeSendResponse send(@CurrentClub Club club, @PathVariable Long eventId) {
		return noticeSendService.sendToDiscord(club, eventId, LocalDateTime.now());
	}
}
