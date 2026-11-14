package com.moa.backend.application.csv;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 적재할 수 있는 구글폼 응답 한 행.
 *
 * @param rowNumber 헤더를 1행으로 센 CSV 레코드 번호
 * @param studentId 비어 있으면 null
 * @param contact 비어 있으면 null
 * @param answers 고정 컬럼을 뺀 나머지 응답 (헤더 순서)
 * @param rowHash 원본 행의 SHA-256
 */
public record FormResponseRow(
		long rowNumber,
		String name,
		String studentId,
		String contact,
		LocalDateTime appliedAt,
		Map<String, String> answers,
		String rowHash
) {
}
