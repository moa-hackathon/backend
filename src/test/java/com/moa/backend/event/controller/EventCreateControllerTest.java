package com.moa.backend.event.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.auth.CurrentClubArgumentResolver;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.dto.EventCreateResponse;
import com.moa.backend.event.service.EventCreateService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventCreateController.class)
class EventCreateControllerTest {

	private static final String INVITE_CODE = "invite-code";
	private static final String NOTICE = "[모아] 네트워킹 행사 안내\n신청 링크: [구글폼 링크]";

	private static final String BODY = """
			{
			  "title": "모아 네트워킹 행사",
			  "eventAt": "2026-11-21T19:00:00",
			  "location": "공학관 302호",
			  "capacity": 40,
			  "minCapacity": 20,
			  "applyDeadline": "2026-11-19T23:59:00",
			  "extraQuestions": ["뒤풀이 참석 여부"],
			  "description": "피자 나와요",
			  "noticeText": "[모아] 네트워킹 행사 안내\\n신청 링크: [구글폼 링크]"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EventCreateService eventCreateService;

	@MockitoBean
	private ClubRepository clubRepository;

	@Test
	void 행사를_만들고_201로_돌려준다() throws Exception {
		Club club = club();
		given(eventCreateService.create(eq(club), argThat(request ->
				request.noticeText().equals(NOTICE) && request.extraQuestions().equals(List.of("뒤풀이 참석 여부")))))
				.willReturn(new EventCreateResponse(10L, "AbCdEfGhIjKlMnOpQrStUv", "모아 네트워킹 행사",
						LocalDateTime.of(2026, 11, 21, 19, 0), "공학관 302호", 40, 20,
						LocalDateTime.of(2026, 11, 19, 23, 59), List.of("뒤풀이 참석 여부"), "피자 나와요", NOTICE));

		mockMvc.perform(post("/api/events")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.shareCode").value("AbCdEfGhIjKlMnOpQrStUv"))
				.andExpect(jsonPath("$.title").value("모아 네트워킹 행사"))
				.andExpect(jsonPath("$.eventAt").value("2026-11-21T19:00:00"))
				.andExpect(jsonPath("$.location").value("공학관 302호"))
				.andExpect(jsonPath("$.capacity").value(40))
				.andExpect(jsonPath("$.minCapacity").value(20))
				.andExpect(jsonPath("$.applyDeadline").value("2026-11-19T23:59:00"))
				.andExpect(jsonPath("$.extraQuestions[0]").value("뒤풀이 참석 여부"))
				.andExpect(jsonPath("$.description").value("피자 나와요"))
				.andExpect(jsonPath("$.noticeText").value(NOTICE));
	}

	@Test
	void 규칙을_어기면_400() throws Exception {
		Club club = club();
		given(eventCreateService.create(eq(club), any()))
				.willThrow(new ApiException(ErrorCode.INVALID_REQUEST, "신청 마감은 행사 일시보다 늦을 수 없습니다."));

		mockMvc.perform(post("/api/events")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, INVITE_CODE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.message").value("신청 마감은 행사 일시보다 늦을 수 없습니다."));
	}

	@Test
	void 초대_코드가_틀리면_401() throws Exception {
		given(clubRepository.findByInviteCode("wrong")).willReturn(Optional.empty());

		mockMvc.perform(post("/api/events")
						.header(CurrentClubArgumentResolver.INVITE_CODE_HEADER, "wrong")
						.contentType(MediaType.APPLICATION_JSON)
						.content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_INVALID"));

		verifyNoInteractions(eventCreateService);
	}

	private Club club() {
		Club club = Club.builder().name("모아").inviteCode(INVITE_CODE).build();
		ReflectionTestUtils.setField(club, "id", 1L);
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));
		return club;
	}
}
