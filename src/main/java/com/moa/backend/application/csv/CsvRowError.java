package com.moa.backend.application.csv;

/**
 * 적재하지 못한 CSV 행.
 *
 * @param rowNumber 헤더를 1행으로 센 CSV 레코드 번호 (첫 응답이 2행)
 * @param reason 사람이 읽을 사유
 */
public record CsvRowError(long rowNumber, String reason) {
}
