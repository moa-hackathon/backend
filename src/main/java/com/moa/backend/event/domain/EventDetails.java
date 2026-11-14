package com.moa.backend.event.domain;

import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 완성된 행사 요약 (F1). 공지 초안 생성과 행사 생성이 같은 검증 규칙을 쓰도록 한곳에 둔다.
 * 만들면서 문자열을 다듬고(trim) 규칙을 확인하며, 어기면 {@link ErrorCode#INVALID_REQUEST}로 실패한다.
 *
 * @param title 행사명
 * @param eventAt 행사 일시
 * @param location 장소
 * @param capacity 정원 (1 이상)
 * @param minCapacity 최소 필수 인원 (0 이상, 정원 이하)
 * @param applyDeadline 신청 마감 (행사 일시 이전)
 * @param extraQuestions 신청받을 때 추가로 물어볼 질문. 빈 항목과 중복은 뺀다
 * @param description 공지에 넣을 기타 안내 (예: 피자 나와요). 없으면 null
 */
public record EventDetails(
		String title,
		LocalDateTime eventAt,
		String location,
		int capacity,
		int minCapacity,
		LocalDateTime applyDeadline,
		List<String> extraQuestions,
		String description
) {

	public static final int TITLE_MAX_LENGTH = 100;
	public static final int LOCATION_MAX_LENGTH = 255;
	public static final int DESCRIPTION_MAX_LENGTH = 1000;
	public static final int EXTRA_QUESTION_MAX_COUNT = 10;
	public static final int EXTRA_QUESTION_MAX_LENGTH = 100;

	public static EventDetails of(String title, LocalDateTime eventAt, String location, Integer capacity,
			Integer minCapacity, LocalDateTime applyDeadline, List<String> extraQuestions, String description) {
		String normalizedTitle = required(title, "행사명", TITLE_MAX_LENGTH);
		String normalizedLocation = required(location, "장소", LOCATION_MAX_LENGTH);
		if (eventAt == null) {
			throw invalid("행사 일시를 입력해 주세요.");
		}
		if (applyDeadline == null) {
			throw invalid("신청 마감일을 입력해 주세요.");
		}
		if (capacity == null) {
			throw invalid("정원을 입력해 주세요.");
		}
		if (capacity < 1) {
			throw invalid("정원은 1명 이상이어야 합니다.");
		}
		int min = minCapacity == null ? 0 : minCapacity;
		if (min < 0 || min > capacity) {
			throw invalid("최소 인원은 0명 이상, 정원 이하여야 합니다.");
		}
		if (applyDeadline.isAfter(eventAt)) {
			throw invalid("신청 마감은 행사 일시보다 늦을 수 없습니다.");
		}
		String normalizedDescription = optional(description, "기타 안내", DESCRIPTION_MAX_LENGTH);
		return new EventDetails(normalizedTitle, eventAt, normalizedLocation, capacity, min, applyDeadline,
				normalizeQuestions(extraQuestions), normalizedDescription);
	}

	private static List<String> normalizeQuestions(List<String> questions) {
		if (questions == null) {
			return List.of();
		}
		Set<String> unique = new LinkedHashSet<>();
		for (String question : questions) {
			if (question == null || question.isBlank()) {
				continue;
			}
			String trimmed = question.trim();
			if (trimmed.length() > EXTRA_QUESTION_MAX_LENGTH) {
				throw invalid("추가 질문은 " + EXTRA_QUESTION_MAX_LENGTH + "자 이하로 입력해 주세요.");
			}
			unique.add(trimmed);
		}
		if (unique.size() > EXTRA_QUESTION_MAX_COUNT) {
			throw invalid("추가 질문은 " + EXTRA_QUESTION_MAX_COUNT + "개까지 넣을 수 있습니다.");
		}
		return List.copyOf(new ArrayList<>(unique));
	}

	private static String required(String value, String label, int maxLength) {
		if (value == null || value.isBlank()) {
			throw invalid(label + "을(를) 입력해 주세요.");
		}
		return limited(value.trim(), label, maxLength);
	}

	private static String optional(String value, String label, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return limited(value.trim(), label, maxLength);
	}

	private static String limited(String value, String label, int maxLength) {
		if (value.length() > maxLength) {
			throw invalid(label + "은(는) " + maxLength + "자 이하로 입력해 주세요.");
		}
		return value;
	}

	private static ApiException invalid(String message) {
		return new ApiException(ErrorCode.INVALID_REQUEST, message);
	}
}
