package com.moa.backend.application.dto;

import com.moa.backend.application.csv.CsvRowError;
import io.swagger.v3.oas.annotations.media.Schema;

public record CsvRowFailureResponse(
		@Schema(description = "CSV 행 번호 (헤더가 1행, 첫 응답이 2행)", example = "5")
		long rowNumber,
		@Schema(description = "적재하지 못한 사유", example = "타임스탬프 형식을 해석할 수 없습니다: 어제 오후")
		String reason
) {

	public static CsvRowFailureResponse from(CsvRowError error) {
		return new CsvRowFailureResponse(error.rowNumber(), error.reason());
	}
}
