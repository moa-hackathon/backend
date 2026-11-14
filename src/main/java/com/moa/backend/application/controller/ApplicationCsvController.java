package com.moa.backend.application.controller;

import com.moa.backend.application.dto.CsvUploadResponse;
import com.moa.backend.application.service.ApplicationCsvImportService;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "신청 CSV", description = "구글폼 응답 CSV 불러오기 (08 대시보드 · 응답 불러오기, 09 CSV 다시 불러오기)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/{eventId}/applications")
public class ApplicationCsvController {

	private final ApplicationCsvImportService applicationCsvImportService;

	@Operation(
			summary = "구글폼 응답 CSV 불러오기",
			description = """
					구글폼 응답 탭에서 내려받은 CSV를 올리면 신청자를 적재합니다.
					UTF-8(BOM 포함)과 엑셀이 다시 저장한 CP949 파일을 읽습니다.
					헤더 이름으로 타임스탬프·이름·학번·연락처 열을 찾고, 나머지 열은 응답 원본(answers)으로 저장합니다.
					같은 파일이나 응답이 늘어난 파일을 다시 올려도 이미 적재된 행은 건너뜁니다.
					적재 후 행사 전체 신청의 확정/대기를 신청 시각 순으로 다시 매기고 마지막 업로드 시각을 갱신합니다.
					타임스탬프를 해석하지 못한 행 등은 적재하지 않고 failures에 행 번호와 사유로 돌려줍니다.""")
	@PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CsvUploadResponse uploadCsv(
			@CurrentClub Club club,
			@Parameter(description = "행사 ID") @PathVariable Long eventId,
			@Parameter(description = "구글폼 응답 CSV 파일") @RequestPart("file") MultipartFile file
	) {
		return applicationCsvImportService.importCsv(club, eventId, readBytes(file));
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw new ApiException(ErrorCode.CSV_UNREADABLE, "업로드한 파일을 읽을 수 없습니다.");
		}
	}
}
