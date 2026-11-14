package com.moa.backend.application.csv;

import java.util.List;

/**
 * 구글폼 CSV를 읽은 결과.
 *
 * @param questions answers 키 목록 (고정 컬럼을 뺀 헤더, 헤더 순서)
 * @param totalRows 헤더와 빈 줄을 뺀 응답 행 수 ({@code rows} + {@code errors})
 * @param rows 적재할 수 있는 행 (파일 순서, 같은 해시가 여러 번 나올 수 있다)
 * @param errors 해석하지 못한 행
 */
public record ParsedFormCsv(List<String> questions, int totalRows, List<FormResponseRow> rows,
		List<CsvRowError> errors) {
}
