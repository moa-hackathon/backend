package com.moa.backend.club.controller;

import com.moa.backend.club.dto.ClubResponse;
import com.moa.backend.club.dto.DiscordWebhookConnectRequest;
import com.moa.backend.club.entity.Club;
import com.moa.backend.club.service.ClubService;
import com.moa.backend.common.auth.CurrentClub;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "동아리", description = "운영진용 동아리 API")
@RestController
@RequestMapping("/api/club")
@RequiredArgsConstructor
public class ClubController {

	private final ClubService clubService;

	@Operation(
			summary = "동아리 정보 조회",
			description = "초대 코드(X-Invite-Code 헤더)의 동아리 정보와 디스코드 연결 여부를 돌려줍니다. "
					+ "웹훅 URL은 토큰이 들어 있어 내려주지 않습니다.")
	@GetMapping
	public ClubResponse getClub(@CurrentClub Club club) {
		return clubService.getClub(club);
	}

	@Operation(
			summary = "디스코드 연결 (웹훅 등록 / 교체)",
			description = "공지를 올릴 디스코드 채널의 웹훅 URL을 등록합니다. 이미 연결돼 있으면 교체합니다. "
					+ "URL 형식이 틀리거나 Discord에 없는 웹훅이면 400 DISCORD_WEBHOOK_INVALID, "
					+ "Discord에 확인하지 못하면 502 DISCORD_SEND_FAILED로 응답합니다. 메시지는 보내지 않습니다.")
	@PutMapping("/discord-webhook")
	public ClubResponse connectDiscordWebhook(@CurrentClub Club club,
			@RequestBody DiscordWebhookConnectRequest request) {
		return clubService.connectDiscordWebhook(club, request.webhookUrl());
	}
}
