package com.moa.backend.dashboard.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.backend.application.entity.ApplicationStatus;
import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.dashboard.domain.AnswerStat;
import com.moa.backend.dashboard.domain.ApplicationFilter;
import com.moa.backend.dashboard.domain.DisplayStatus;
import com.moa.backend.dashboard.domain.Banner;
import com.moa.backend.dashboard.domain.NextAction;
import com.moa.backend.dashboard.domain.RequiredField;
import com.moa.backend.dashboard.dto.ApplicationListResponse;
import com.moa.backend.dashboard.dto.ApplicationRowResponse;
import com.moa.backend.dashboard.dto.CheckSummaryResponse;
import com.moa.backend.dashboard.dto.DashboardResponse;
import com.moa.backend.dashboard.dto.EventSummaryResponse;
import com.moa.backend.dashboard.export.ExcelDownload;
import com.moa.backend.dashboard.service.DashboardService;
import com.moa.backend.event.domain.EventProgress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

	private static final String INVITE_CODE = "invite-code";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 12, 40);

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private DashboardService dashboardService;
	@MockitoBean
	private ClubRepository clubRepository;

	private Club club;

	@BeforeEach
	void setUp() {
		club = Club.builder().name("모아").inviteCode(INVITE_CODE).build();
		ReflectionTestUtils.setField(club, "id", 1L);
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));
	}

	@Test
	void 대시보드_요약을_조회한다() throws Exception {
		EventProgress progress = EventProgress.of(34, 40, 20, LocalDateTime.of(2026, 11, 19, 23, 59), NOW);
		DashboardResponse response = new DashboardResponse(
				new EventSummaryResponse(10L, "모아 네트워킹 행사", LocalDateTime.of(2026, 11, 21, 19, 0), "공학관 302호", NOW),
				progress,
				Banner.from(progress),
				NextAction.from(progress),
				List.of(new AnswerStat("뒤풀이 참석 여부", 34,
						List.of(new AnswerStat.Option("참석", 22, 65), new AnswerStat.Option("불참", 12, 35)))),
				new CheckSummaryResponse(2, 1,
						List.of(new CheckSummaryResponse.MissingFieldCount(RequiredField.CONTACT, "연락처", 1)), "연락처 없음"));
		given(dashboardService.getDashboard(eq(club), eq(10L), any(LocalDateTime.class))).willReturn(response);

		mockMvc.perform(get("/api/events/10/dashboard").header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.event.title").value("모아 네트워킹 행사"))
				.andExpect(jsonPath("$.banner.title").value("진행 가능 · 정원 임박"))
				.andExpect(jsonPath("$.banner.message").value("최소 인원은 넘었고, 정원까지 6명 남았어요."))
				.andExpect(jsonPath("$.event.lastSyncedAt").value("2026-11-14T12:40:00"))
				.andExpect(jsonPath("$.progress.applicantCount").value(34))
				.andExpect(jsonPath("$.progress.status").value("NEAR_CAPACITY"))
				.andExpect(jsonPath("$.progress.remainingToCapacity").value(6))
				.andExpect(jsonPath("$.nextAction.type").value("CLOSING_SOON"))
				.andExpect(jsonPath("$.nextAction.message").value("정원까지 6명 남았어요. 마감 임박 공지를 만들어 드릴까요?"))
				.andExpect(jsonPath("$.answerStats[0].options[0].value").value("참석"))
				.andExpect(jsonPath("$.answerStats[0].options[0].percent").value(65))
				.andExpect(jsonPath("$.checks.duplicateCount").value(2))
				.andExpect(jsonPath("$.checks.missingFields[0].field").value("CONTACT"))
				.andExpect(jsonPath("$.checks.missingSummary").value("연락처 없음"));
	}

	@Test
	void 초대_코드가_없으면_401() throws Exception {
		mockMvc.perform(get("/api/events/10/dashboard"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_REQUIRED"));
	}

	@Test
	void 소유하지_않은_행사면_404() throws Exception {
		given(dashboardService.getDashboard(eq(club), eq(99L), any(LocalDateTime.class)))
				.willThrow(new ApiException(ErrorCode.EVENT_NOT_FOUND));

		mockMvc.perform(get("/api/events/99/dashboard").header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
	}

	@Test
	void 신청자_목록을_필터로_조회한다() throws Exception {
		ApplicationRowResponse row = new ApplicationRowResponse(4L, "김모아", "20230015", "010-1234-5678",
				Map.of("뒤풀이 참석 여부", "참석"), LocalDateTime.of(2026, 11, 14, 11, 20), ApplicationStatus.WAITING,
				DisplayStatus.DUPLICATE, "중복", true, List.of());
		given(dashboardService.getApplications(club, 10L, ApplicationFilter.NEEDS_CHECK))
				.willReturn(new ApplicationListResponse(ApplicationFilter.NEEDS_CHECK, 1, 34, List.of(row)));

		mockMvc.perform(get("/api/events/10/applications").param("filter", "NEEDS_CHECK")
						.header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.filter").value("NEEDS_CHECK"))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.applications[0].contact").value("010-1234-5678"))
				.andExpect(jsonPath("$.applications[0].answers['뒤풀이 참석 여부']").value("참석"))
				.andExpect(jsonPath("$.applications[0].displayStatus").value("DUPLICATE"))
				.andExpect(jsonPath("$.applications[0].displayStatusLabel").value("중복"));
	}

	@Test
	void 필터를_생략하면_전체를_조회한다() throws Exception {
		given(dashboardService.getApplications(club, 10L, ApplicationFilter.ALL))
				.willReturn(new ApplicationListResponse(ApplicationFilter.ALL, 0, 0, List.of()));

		mockMvc.perform(get("/api/events/10/applications").header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.filter").value("ALL"));

		verify(dashboardService).getApplications(club, 10L, ApplicationFilter.ALL);
	}

	@Test
	void 알_수_없는_필터면_400() throws Exception {
		mockMvc.perform(get("/api/events/10/applications").param("filter", "NOPE")
						.header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 엑셀을_한글_파일명으로_내려준다() throws Exception {
		byte[] bytes = {1, 2, 3};
		given(dashboardService.exportApplications(club, 10L))
				.willReturn(ExcelDownload.forEvent(10L, "모아 행사", bytes));

		mockMvc.perform(get("/api/events/10/applications/export").header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(content().contentType(ExcelDownload.CONTENT_TYPE))
				.andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"event-10-applications.xlsx\"")))
				.andExpect(header().string("Content-Disposition",
						endsWith("filename*=UTF-8''%EB%AA%A8%EC%95%84%20%ED%96%89%EC%82%AC_%EC%8B%A0%EC%B2%AD%EC%9E%90.xlsx")))
				.andExpect(content().bytes(bytes));
	}
}
