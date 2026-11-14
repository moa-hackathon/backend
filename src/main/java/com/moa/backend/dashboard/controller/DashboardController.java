package com.moa.backend.dashboard.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.dashboard.domain.ApplicationFilter;
import com.moa.backend.dashboard.dto.ApplicationListResponse;
import com.moa.backend.dashboard.dto.DashboardResponse;
import com.moa.backend.dashboard.export.ExcelDownload;
import com.moa.backend.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대시보드", description = "행사 진행 대시보드와 신청자 응답 조회")
@RestController
@RequestMapping("/api/events/{eventId}")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	@Operation(summary = "대시보드 요약 조회",
			description = "행사 정보, 진행 상황(신청 인원·정원·최소 인원·D-day), 다음 할 일 제안, "
					+ "조건별 응답 집계, 확인 필요 항목(중복·필수 항목 미기입)을 한 번에 조회한다.")
	@GetMapping("/dashboard")
	public DashboardResponse getDashboard(@CurrentClub Club club, @PathVariable Long eventId) {
		return dashboardService.getDashboard(club, eventId, LocalDateTime.now());
	}

	@Operation(summary = "신청자 응답 목록 조회",
			description = "신청 시각 오름차순으로 신청자 응답을 조회한다. 행마다 확정/대기/중복/누락/취소 중 하나의 표시 상태가 붙는다. "
					+ "filter: ALL(취소 포함 전체), CONFIRMED(확정), WAITING(대기), NEEDS_CHECK(중복 또는 누락).")
	@GetMapping("/applications")
	public ApplicationListResponse getApplications(
			@CurrentClub Club club,
			@PathVariable Long eventId,
			@Parameter(description = "목록 필터") @RequestParam(defaultValue = "ALL") ApplicationFilter filter
	) {
		return dashboardService.getApplications(club, eventId, filter);
	}

	@Operation(summary = "신청자 응답 엑셀 다운로드",
			description = "신청자 전체(취소 포함)를 .xlsx 한 시트로 내려준다. 열: 이름, 학번, 연락처, 폼 문항들, 신청 시각, 상태.")
	@GetMapping("/applications/export")
	public ResponseEntity<byte[]> exportApplications(@CurrentClub Club club, @PathVariable Long eventId) {
		ExcelDownload download = dashboardService.exportApplications(club, eventId);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, download.contentDisposition())
				.contentType(MediaType.parseMediaType(ExcelDownload.CONTENT_TYPE))
				.contentLength(download.content().length)
				.body(download.content());
	}
}
