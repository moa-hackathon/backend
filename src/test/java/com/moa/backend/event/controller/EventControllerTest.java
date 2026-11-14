package com.moa.backend.event.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.auth.CurrentClubArgumentResolver;
import com.moa.backend.event.domain.EventProgressStatus;
import com.moa.backend.event.dto.EventCardResponse;
import com.moa.backend.event.dto.EventListResponse;
import com.moa.backend.event.service.EventQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
class EventControllerTest {

	private static final String INVITE_CODE = "invite-code";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EventQueryService eventQueryService;

	@MockitoBean
	private ClubRepository clubRepository;

	@Test
	void 진행_중인_행사_목록을_돌려준다() throws Exception {
		Club club = Club.builder().name("모아").inviteCode(INVITE_CODE).build();
		ReflectionTestUtils.setField(club, "id", 1L);
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));
		EventCardResponse card = new EventCardResponse(10L, "모아 네트워킹 행사", LocalDateTime.of(2026, 11, 21, 19, 0),
				"공학관 302호", 34, 40, 20, LocalDateTime.of(2026, 11, 19, 23, 59), EventProgressStatus.NEAR_CAPACITY,
				0, 6, 85, 5, false, "정원까지 6명 남음");
		given(eventQueryService.getOngoingEvents(eq(club), any(LocalDateTime.class)))
				.willReturn(EventListResponse.of(List.of(card)));

		mockMvc.perform(get("/api/events").header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.events[0].id").value(10))
				.andExpect(jsonPath("$.events[0].title").value("모아 네트워킹 행사"))
				.andExpect(jsonPath("$.events[0].eventAt").value("2026-11-21T19:00:00"))
				.andExpect(jsonPath("$.events[0].location").value("공학관 302호"))
				.andExpect(jsonPath("$.events[0].applicantCount").value(34))
				.andExpect(jsonPath("$.events[0].capacity").value(40))
				.andExpect(jsonPath("$.events[0].minCapacity").value(20))
				.andExpect(jsonPath("$.events[0].applyDeadline").value("2026-11-19T23:59:00"))
				.andExpect(jsonPath("$.events[0].status").value("NEAR_CAPACITY"))
				.andExpect(jsonPath("$.events[0].remainingToMin").value(0))
				.andExpect(jsonPath("$.events[0].remainingToCapacity").value(6))
				.andExpect(jsonPath("$.events[0].fillPercent").value(85))
				.andExpect(jsonPath("$.events[0].daysUntilDeadline").value(5))
				.andExpect(jsonPath("$.events[0].deadlinePassed").value(false))
				.andExpect(jsonPath("$.events[0].hint").value("정원까지 6명 남음"));
	}

	@Test
	void 초대_코드가_없으면_401() throws Exception {
		mockMvc.perform(get("/api/events"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_REQUIRED"));

		verifyNoInteractions(eventQueryService);
	}

	@Test
	void 초대_코드가_틀리면_401() throws Exception {
		given(clubRepository.findByInviteCode("wrong")).willReturn(Optional.empty());

		mockMvc.perform(get("/api/events").header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, "wrong"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_INVALID"));

		verifyNoInteractions(eventQueryService);
	}
}
