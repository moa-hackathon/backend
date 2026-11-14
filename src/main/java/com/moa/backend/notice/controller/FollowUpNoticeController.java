package com.moa.backend.notice.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.notice.dto.FinalNoticeResponse;
import com.moa.backend.notice.dto.FollowUpNoticeDraftRequest;
import com.moa.backend.notice.dto.FollowUpNoticeDraftResponse;
import com.moa.backend.notice.dto.NoticeUpdateRequest;
import com.moa.backend.notice.service.FollowUpNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "후속 공지", description = "대시보드 다음 할 일(F11)의 후속 공지 API")
@RestController
@RequestMapping("/api/events/{eventId}/notice")
@RequiredArgsConstructor
public class FollowUpNoticeController {

	private final FollowUpNoticeService followUpNoticeService;

	@Operation(
			summary = "후속 공지 초안 생성",
			description = "현재 신청 현황으로 후속 공지(추가 모집, 신청 독려, 마감 임박, 마감 안내) 초안을 만듭니다. "
					+ "type을 비우면 진행 상태로 정합니다. 저장하지 않으며, 확정하려면 공지 본문 교체 API를 호출합니다.")
	@PostMapping("/follow-up/draft")
	public FollowUpNoticeDraftResponse draft(@CurrentClub Club club, @PathVariable Long eventId,
			@RequestBody(required = false) FollowUpNoticeDraftRequest request) {
		return followUpNoticeService.draft(club, eventId, request == null ? null : request.type(),
				LocalDateTime.now());
	}

	@Operation(
			summary = "공지 본문 교체",
			description = "행사의 공지 본문을 바꿉니다. 이후 최종 공지 조회와 발송 API가 이 본문을 씁니다. "
					+ "본문에 [구글폼 링크] 자리가 없으면 신청 링크 줄을 덧붙입니다.")
	@PutMapping
	public FinalNoticeResponse updateNotice(@CurrentClub Club club, @PathVariable Long eventId,
			@Valid @RequestBody NoticeUpdateRequest request) {
		return followUpNoticeService.updateNotice(club, eventId, request.noticeText());
	}
}
