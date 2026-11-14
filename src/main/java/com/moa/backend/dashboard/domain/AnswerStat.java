package com.moa.backend.dashboard.domain;

import java.util.List;

/**
 * 문항 하나의 응답 집계.
 *
 * @param question 문항 (구글폼 열 이름)
 * @param answeredCount 이 문항에 값을 적은 신청 수 (취소 제외)
 * @param options 응답 값별 인원, 많은 순 (같으면 먼저 나온 값 순)
 */
public record AnswerStat(String question, int answeredCount, List<Option> options) {

	/**
	 * @param percent 신청 인원(취소 제외) 대비 비율(%), 반올림
	 */
	public record Option(String value, int count, int percent) {
	}
}
