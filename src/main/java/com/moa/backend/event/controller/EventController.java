package com.moa.backend.event.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.event.dto.EventListResponse;
import com.moa.backend.event.service.EventQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "행사", description = "운영진용 행사 API")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

	private final EventQueryService eventQueryService;

	@Operation(
			summary = "진행 중인 행사 목록 조회 (홈)",
			description = "초대 코드(X-Invite-Code 헤더)의 동아리가 가진 행사 중 행사 일시가 아직 지나지 않은 행사를 "
					+ "행사 일시 오름차순으로 돌려줍니다. 카드마다 신청 인원(취소 제외), 진행 상태, 남은 인원, "
					+ "신청 마감 D-day, 카드 하단 안내 문구를 담습니다.")
	@GetMapping
	public EventListResponse getOngoingEvents(@CurrentClub Club club) {
		return eventQueryService.getOngoingEvents(club, LocalDateTime.now());
	}
}
