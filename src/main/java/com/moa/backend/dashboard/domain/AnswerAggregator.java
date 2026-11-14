package com.moa.backend.dashboard.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 조건별 응답 집계(F8). 취소되지 않은 신청의 {@code answers}를 문항별·값별로 센다.
 *
 * <p>객관식처럼 보이는 문항만 집계한다. 서술형 문항이 섞이면 값 종류가 응답 수만큼 늘어나므로
 * 아래 두 조건을 모두 만족하는 문항만 남긴다.
 * <ul>
 *   <li>서로 다른 값이 {@value #MAX_DISTINCT_VALUES}개 이하</li>
 *   <li>가장 긴 값이 {@value #MAX_VALUE_LENGTH}자 이하</li>
 * </ul>
 * 값은 앞뒤 공백을 떼고 비교하며, 빈 값은 응답하지 않은 것으로 본다.
 */
public final class AnswerAggregator {

	public static final int MAX_DISTINCT_VALUES = 10;
	public static final int MAX_VALUE_LENGTH = 30;

	private AnswerAggregator() {
	}

	public static List<AnswerStat> aggregate(ApplicationReview review) {
		List<ReviewedApplication> active = review.activeRows();
		int applicantCount = active.size();
		List<AnswerStat> stats = new ArrayList<>();
		for (String question : review.questions()) {
			Map<String, Integer> counts = new LinkedHashMap<>();
			int answered = 0;
			for (ReviewedApplication row : active) {
				Map<String, String> answers = row.application().getAnswers();
				String value = answers == null ? null : answers.get(question);
				if (value == null || value.isBlank()) {
					continue;
				}
				counts.merge(value.strip(), 1, Integer::sum);
				answered++;
			}
			if (isChoiceLike(counts)) {
				stats.add(new AnswerStat(question, answered, toOptions(counts, applicantCount)));
			}
		}
		return stats;
	}

	private static boolean isChoiceLike(Map<String, Integer> counts) {
		return !counts.isEmpty()
				&& counts.size() <= MAX_DISTINCT_VALUES
				&& counts.keySet().stream().allMatch(value -> value.length() <= MAX_VALUE_LENGTH);
	}

	private static List<AnswerStat.Option> toOptions(Map<String, Integer> counts, int applicantCount) {
		// LinkedHashMap의 등장 순서를 유지한 채 안정 정렬한다.
		return counts.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
				.map(entry -> new AnswerStat.Option(entry.getKey(), entry.getValue(),
						percent(entry.getValue(), applicantCount)))
				.toList();
	}

	private static int percent(int count, int total) {
		return total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
	}
}
