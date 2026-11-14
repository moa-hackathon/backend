package com.moa.backend.event.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.event.dto.EventCreateRequest;
import com.moa.backend.event.dto.EventCreateResponse;
import com.moa.backend.event.service.EventCreateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "행사", description = "운영진용 행사 API")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventCreateController {

	private final EventCreateService eventCreateService;

	@Operation(
			summary = "행사 만들기 (이대로 확인)",
			description = "행사 요약, 확정한 공지 본문, 추가 질문을 저장하고 부원용 공유 코드(shareCode)를 발급합니다. "
					+ "정원은 1 이상, 최소 인원은 0 이상 정원 이하, 신청 마감은 행사 일시 이전이어야 합니다. "
					+ "공지에 [구글폼 링크] 자리가 없으면 마지막 줄에 붙입니다.")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EventCreateResponse create(@CurrentClub Club club, @RequestBody EventCreateRequest request) {
		return eventCreateService.create(club, request);
	}
}
