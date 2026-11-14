package com.moa.backend.event.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.dto.EventDetailResponse;
import com.moa.backend.event.repository.EventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "행사", description = "운영진용 행사 API")
@RestController
@RequestMapping("/api/events/{eventId}")
@RequiredArgsConstructor
public class EventDetailController {

	private final EventRepository eventRepository;

	@Operation(
			summary = "행사 단건 조회",
			description = "행사 요약과 구글폼 링크, 공지 발송 시각을 돌려줍니다. 다른 동아리의 행사면 404 EVENT_NOT_FOUND입니다.")
	@GetMapping
	public EventDetailResponse getEvent(@CurrentClub Club club, @PathVariable Long eventId) {
		return eventRepository.findByIdAndClubId(eventId, club.getId())
				.map(EventDetailResponse::from)
				.orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND));
	}
}
