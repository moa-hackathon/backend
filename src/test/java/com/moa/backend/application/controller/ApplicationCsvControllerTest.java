package com.moa.backend.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moa.backend.application.dto.CsvRowFailureResponse;
import com.moa.backend.application.dto.CsvUploadResponse;
import com.moa.backend.application.service.ApplicationCsvImportService;
import com.moa.backend.club.entity.Club;
import com.moa.backend.club.repository.ClubRepository;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApplicationCsvController.class)
class ApplicationCsvControllerTest {

	private static final String URL = "/api/events/{eventId}/applications/csv";
	private static final String INVITE_CODE = "invite-code";
	private static final byte[] CSV = "타임스탬프,이름\n2026/11/14 9:12:00 오전 GMT+9,김모아\n"
			.getBytes(StandardCharsets.UTF_8);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ApplicationCsvImportService service;

	@MockitoBean
	private ClubRepository clubRepository;

	private final Club club = Club.builder().name("모아").inviteCode(INVITE_CODE).build();

	@BeforeEach
	void setUp() {
		given(clubRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(club));
	}

	@Test
	void 업로드_결과를_돌려준다() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "responses.csv", "text/csv", CSV);
		given(service.importCsv(eq(club), eq(10L), any())).willReturn(new CsvUploadResponse(
				3, 2, 0, List.of(new CsvRowFailureResponse(4, "타임스탬프 형식을 해석할 수 없습니다: 어제")),
				2, 0, LocalDateTime.of(2026, 11, 14, 12, 40)));

		mockMvc.perform(multipart(URL, 10L).file(file).header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalRows").value(3))
				.andExpect(jsonPath("$.insertedCount").value(2))
				.andExpect(jsonPath("$.skippedCount").value(0))
				.andExpect(jsonPath("$.failures[0].rowNumber").value(4))
				.andExpect(jsonPath("$.failures[0].reason").value("타임스탬프 형식을 해석할 수 없습니다: 어제"))
				.andExpect(jsonPath("$.confirmedCount").value(2))
				.andExpect(jsonPath("$.waitingCount").value(0))
				.andExpect(jsonPath("$.lastSyncedAt").value("2026-11-14T12:40:00"));

		verify(service).importCsv(eq(club), eq(10L), eq(CSV));
	}

	@Test
	void 서비스의_CSV_오류는_400과_코드로_내려간다() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "x.csv", "text/csv", CSV);
		given(service.importCsv(any(), any(), any()))
				.willThrow(new ApiException(ErrorCode.CSV_COLUMN_MISSING, "이름 열을 찾을 수 없습니다."));

		mockMvc.perform(multipart(URL, 10L).file(file).header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CSV_COLUMN_MISSING"))
				.andExpect(jsonPath("$.message").value("이름 열을 찾을 수 없습니다."));
	}

	@Test
	void 행사가_없으면_404() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "x.csv", "text/csv", CSV);
		given(service.importCsv(any(), any(), any())).willThrow(new ApiException(ErrorCode.EVENT_NOT_FOUND));

		mockMvc.perform(multipart(URL, 10L).file(file).header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
	}

	@Test
	void file_파트가_없으면_400() throws Exception {
		MockMultipartFile other = new MockMultipartFile("other", "x.csv", "text/csv", CSV);

		mockMvc.perform(multipart(URL, 10L).file(other).header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		verifyNoInteractions(service);
	}

	@Test
	void multipart가_아니면_415() throws Exception {
		mockMvc.perform(post(URL, 10L).contentType(MediaType.TEXT_PLAIN).content(CSV)
						.header("X-Invite-Code", INVITE_CODE))
				.andExpect(status().isUnsupportedMediaType());
		verifyNoInteractions(service);
	}

	@Test
	void 초대_코드가_없으면_401() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "x.csv", "text/csv", CSV);

		mockMvc.perform(multipart(URL, 10L).file(file))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVITE_CODE_REQUIRED"));
		verifyNoInteractions(service);
	}
}
