package com.moa.backend.application.csv;

import java.util.List;

/**
 * 구글폼 CSV를 읽은 결과.
 *
 * @param totalRows 헤더와 빈 줄을 뺀 응답 행 수 ({@code rows} + {@code errors})
 * @param rows 적재할 수 있는 행 (파일 순서, 같은 해시가 여러 번 나올 수 있다)
 * @param errors 해석하지 못한 행
 */
public record ParsedFormCsv(int totalRows, List<FormResponseRow> rows, List<CsvRowError> errors) {
}
