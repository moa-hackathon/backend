package com.moa.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record CsvUploadResponse(
		@Schema(description = "헤더와 빈 줄을 뺀 응답 행 수 (= 새로 넣은 수 + 건너뛴 수 + 실패 수)", example = "34")
		int totalRows,
		@Schema(description = "이번에 새로 넣은 신청 수", example = "5")
		int insertedCount,
		@Schema(description = "이미 적재됐거나 파일 안에서 똑같이 반복된 행이라 건너뛴 수", example = "28")
		int skippedCount,
		@Schema(description = "적재하지 못한 행")
		List<CsvRowFailureResponse> failures,
		@Schema(description = "재계산 후 행사의 확정 인원", example = "33")
		int confirmedCount,
		@Schema(description = "재계산 후 행사의 대기 인원 (중복 학번 포함)", example = "0")
		int waitingCount,
		@Schema(description = "마지막 CSV 업로드 시각", example = "2026-11-14T12:40:00")
		LocalDateTime lastSyncedAt
) {
}
