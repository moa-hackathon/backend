package com.moa.backend.dashboard.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.dashboard.dto.ApplicationCancelResponse;
import com.moa.backend.dashboard.service.ApplicationCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대시보드", description = "행사 진행 대시보드와 신청자 응답 조회")
@RestController
@RequestMapping("/api/events/{eventId}/applications")
@RequiredArgsConstructor
public class ApplicationCheckController {

	private final ApplicationCheckService applicationCheckService;

	@Operation(summary = "중복 신청 정리",
			description = "대시보드 \"정리하기\". 같은 학번의 신청 중 가장 빠른 것만 남기고 나머지를 취소한 뒤 확정/대기를 다시 매긴다. "
					+ "중복이 없으면 canceledCount가 0이다.")
	@PostMapping("/duplicates/resolve")
	public ApplicationCancelResponse resolveDuplicates(@CurrentClub Club club, @PathVariable Long eventId) {
		return applicationCheckService.resolveDuplicates(club, eventId);
	}

	@Operation(summary = "신청 취소",
			description = "신청 한 건을 취소한 뒤 확정/대기를 다시 매긴다. 행은 지우지 않고 상태만 CANCELED로 바꾼다. "
					+ "이미 취소된 신청이면 canceledCount가 0이다.")
	@PatchMapping("/{applicationId}/cancel")
	public ApplicationCancelResponse cancel(@CurrentClub Club club, @PathVariable Long eventId,
			@PathVariable Long applicationId) {
		return applicationCheckService.cancel(club, eventId, applicationId);
	}
}
