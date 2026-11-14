package com.moa.backend.application.csv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 구글폼 CSV 헤더에서 고정 컬럼(타임스탬프, 이름, 학번, 연락처) 위치를 찾고, 나머지 열에 answers 키를 붙인다.
 *
 * <p>헤더는 공백을 지우고 소문자로 바꾼 뒤 비교한다. 각 항목은 우선순위 단계(예: 연락처는 전화번호 → 이메일)마다
 * 정확히 일치하는 헤더를 먼저, 키워드를 포함한 헤더를 다음으로 찾는다. 같은 조건에서는 왼쪽 열이 이긴다.
 * "팀 이름", "보호자 연락처"처럼 키워드 바로 앞에 다른 대상을 가리키는 말이 붙은 헤더는 건너뛴다.
 * 한 열은 한 항목에만 쓰이고, 타임스탬프 → 이름 → 학번 → 연락처 순서로 배정한다.
 *
 * @param headers 원본 헤더 (BOM·앞뒤 공백 제거)
 * @param timestamp 타임스탬프 열 위치
 * @param name 이름 열 위치
 * @param studentId 학번 열 위치, 없으면 -1
 * @param contact 연락처 열 위치, 없으면 -1
 * @param answerKeys 고정 컬럼이 아닌 열의 위치 → answers 키 (헤더 순서)
 */
public record FormColumns(
		List<String> headers,
		int timestamp,
		int name,
		int studentId,
		int contact,
		Map<Integer, String> answerKeys
) {

	public static final int NONE = -1;

	private static final Rule TIMESTAMP = new Rule(
			List.of(new Tier(Set.of("타임스탬프", "timestamp"), List.of("타임스탬프", "timestamp"))),
			List.of(),
			List.of());

	/** 키워드 바로 앞에 붙으면 신청자 본인의 값이 아닌 것으로 보는 말 */
	private static final List<String> OTHER_PERSON_OR_THING = List.of(
			"팀", "동아리", "학교", "학과", "전공", "소속", "단체", "회사", "부서", "행사", "모임",
			"추천인", "보호자", "부모님", "부모", "입금자", "예금주", "닉", "영문", "영어", "파일", "계정");

	private static final Rule NAME = new Rule(
			List.of(new Tier(
					Set.of("이름", "성명", "성함", "name", "fullname", "yourname", "이름(name)", "name(이름)"),
					List.of("이름", "성명", "성함"))),
			OTHER_PERSON_OR_THING,
			List.of("영문", "영어", "english"));

	private static final Rule STUDENT_ID = new Rule(
			List.of(new Tier(
					Set.of("학번", "studentid", "studentnumber", "studentno", "studentno."),
					List.of("학번", "studentid", "studentnumber"))),
			OTHER_PERSON_OR_THING,
			List.of());

	/** 전화번호 계열을 먼저 찾고, 없을 때만 이메일 열을 연락처로 쓴다. */
	private static final Rule CONTACT = new Rule(
			List.of(
					new Tier(
							Set.of("연락처", "전화번호", "휴대폰번호", "핸드폰번호", "휴대전화번호", "휴대폰", "핸드폰",
									"휴대전화", "phone", "phonenumber", "contact", "mobile"),
							List.of("연락처", "전화번호", "휴대폰", "핸드폰", "휴대전화", "phonenumber", "contact")),
					new Tier(
							Set.of("이메일", "email", "e-mail", "이메일주소", "emailaddress"),
							List.of("이메일주소", "메일주소", "emailaddress"))),
			List.of("비상", "긴급", "보호자", "부모님", "부모", "추천인", "팀장", "대표자"),
			List.of());

	public static FormColumns resolve(List<String> rawHeaders) {
		List<String> headers = new ArrayList<>(rawHeaders.size());
		List<String> normalized = new ArrayList<>(rawHeaders.size());
		for (String raw : rawHeaders) {
			String header = raw == null ? "" : raw.replace("﻿", "").strip();
			headers.add(header);
			normalized.add(header.replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
		}

		Set<Integer> taken = new HashSet<>();
		int timestamp = TIMESTAMP.find(normalized, taken);
		int name = NAME.find(normalized, taken);
		int studentId = STUDENT_ID.find(normalized, taken);
		int contact = CONTACT.find(normalized, taken);

		if (name == NONE) {
			throw new CsvReadException(CsvReadException.Reason.COLUMN_MISSING,
					"이름 열을 찾을 수 없습니다. 헤더에 '이름'이 들어간 열이 있어야 합니다.");
		}
		if (timestamp == NONE) {
			throw new CsvReadException(CsvReadException.Reason.COLUMN_MISSING,
					"타임스탬프 열을 찾을 수 없습니다. 구글폼 응답 탭에서 내려받은 CSV를 올려 주세요.");
		}

		Map<Integer, String> answerKeys = new LinkedHashMap<>();
		Set<String> usedKeys = new HashSet<>();
		for (int i = 0; i < headers.size(); i++) {
			if (taken.contains(i)) {
				continue;
			}
			String base = headers.get(i).isEmpty() ? "열 " + (i + 1) : headers.get(i);
			String key = base;
			for (int n = 2; !usedKeys.add(key); n++) {
				key = base + " (" + n + ")";
			}
			answerKeys.put(i, key);
		}
		return new FormColumns(Collections.unmodifiableList(headers), timestamp, name, studentId, contact,
				Collections.unmodifiableMap(answerKeys));
	}

	public int width() {
		return headers.size();
	}

	/**
	 * @param exact 공백 제거·소문자 헤더와 정확히 같으면 고르는 값
	 * @param contains 정확히 같은 헤더가 없을 때 포함 여부로 찾는 키워드
	 */
	private record Tier(Set<String> exact, List<String> contains) {
	}

	/**
	 * @param tiers 우선순위 순서의 단계. 각 단계에서 정확히 일치를 먼저, 포함을 다음으로 본다.
	 * @param qualifiers 키워드 바로 앞에 붙으면 제외하는 말 ("의"를 사이에 둔 경우 포함)
	 * @param excludedAnywhere 포함 검사에서 헤더 어디에든 있으면 제외하는 말
	 */
	private record Rule(List<Tier> tiers, List<String> qualifiers, List<String> excludedAnywhere) {

		int find(List<String> normalized, Set<Integer> taken) {
			for (Tier tier : tiers) {
				for (int i = 0; i < normalized.size(); i++) {
					if (!taken.contains(i) && tier.exact().contains(normalized.get(i))) {
						taken.add(i);
						return i;
					}
				}
				for (int i = 0; i < normalized.size(); i++) {
					if (!taken.contains(i) && matches(normalized.get(i), tier.contains())) {
						taken.add(i);
						return i;
					}
				}
			}
			return NONE;
		}

		private boolean matches(String header, List<String> keywords) {
			for (String excluded : excludedAnywhere) {
				if (header.contains(excluded)) {
					return false;
				}
			}
			for (String keyword : keywords) {
				for (int at = header.indexOf(keyword); at >= 0; at = header.indexOf(keyword, at + 1)) {
					if (!qualified(header.substring(0, at))) {
						return true;
					}
				}
			}
			return false;
		}

		private boolean qualified(String prefix) {
			String trimmed = prefix.endsWith("의") ? prefix.substring(0, prefix.length() - 1) : prefix;
			for (String qualifier : qualifiers) {
				if (trimmed.endsWith(qualifier)) {
					return true;
				}
			}
			return false;
		}
	}
}
