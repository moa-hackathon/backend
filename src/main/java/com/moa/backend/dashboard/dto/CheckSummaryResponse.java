package com.moa.backend.dashboard.dto;

import com.moa.backend.dashboard.domain.ApplicationReview;
import com.moa.backend.dashboard.domain.RequiredField;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 확인 필요 항목(F9). 한 행이 중복이면서 누락일 수 있어 두 건수는 따로 센다.
 *
 * @param duplicateCount 중복 신청 건수
 * @param missingCount 필수 항목(이름·학번·연락처) 중 하나라도 빈 신청 건수
 * @param missingFields 항목별로 비어 있는 건수, 빈 건이 있는 항목만
 * @param missingSummary 화면용 요약 (예: "연락처 없음", "연락처 2건, 학번 1건 없음"), 누락이 없으면 null
 */
public record CheckSummaryResponse(
		int duplicateCount,
		int missingCount,
		List<MissingFieldCount> missingFields,
		String missingSummary
) {

	public record MissingFieldCount(RequiredField field, String label, int count) {
	}

	public static CheckSummaryResponse from(ApplicationReview review) {
		Map<RequiredField, Integer> counts = review.missingFieldCounts();
		List<MissingFieldCount> fields = counts.entrySet().stream()
				.map(entry -> new MissingFieldCount(entry.getKey(), entry.getKey().getLabel(), entry.getValue()))
				.toList();
		return new CheckSummaryResponse(review.duplicateCount(), review.missingCount(), fields, summarize(fields));
	}

	private static String summarize(List<MissingFieldCount> fields) {
		if (fields.isEmpty()) {
			return null;
		}
		if (fields.size() == 1) {
			return fields.get(0).label() + " 없음";
		}
		return fields.stream()
				.map(field -> field.label() + " " + field.count() + "건")
				.collect(Collectors.joining(", ", "", " 없음"));
	}
}
