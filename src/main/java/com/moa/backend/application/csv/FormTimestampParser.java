package com.moa.backend.application.csv;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 구글폼 응답 CSV의 타임스탬프를 읽는다. 형식이 스프레드시트 로케일마다 달라서 고정 패턴 대신
 * 숫자 덩어리와 오전/오후 표시를 뽑아 해석한다.
 *
 * <p>지원 예시:
 * <ul>
 *   <li>{@code 2026/11/14 9:12:00 오전 GMT+9}, {@code 2026/11/14 9:12:00 AM GMT+9}</li>
 *   <li>{@code 2026. 11. 14 오전 9:12:00}, {@code 2026. 11. 14. 오후 1:05:09}</li>
 *   <li>{@code 2026-11-14 09:12:00}, {@code 2026-11-14T09:12:00+09:00}, {@code 2026-11-14 9:12}</li>
 *   <li>{@code 11/14/2026 9:12:00}, {@code 11/14/2026 9:12:00 PM}</li>
 *   <li>{@code 14/11/2026 21:12:00} (파일 안에 일이 12를 넘는 값이 있어 일/월 순서가 드러날 때)</li>
 *   <li>{@code 2026년 11월 14일 오후 9시 12분}</li>
 * </ul>
 *
 * <p>시간대 표시(GMT+9, UTC, KST, +09:00)는 버리고 적힌 벽시계 시각을 그대로 쓴다.
 * 시간대 표시가 없는 형식과 결과를 맞추기 위해서다.
 */
public final class FormTimestampParser {

	private static final Pattern AM = Pattern.compile("오전|(?i:(?<![A-Za-z])A\\.?M\\.?(?![A-Za-z]))");
	private static final Pattern PM = Pattern.compile("오후|(?i:(?<![A-Za-z])P\\.?M\\.?(?![A-Za-z]))");
	private static final Pattern NAMED_ZONE = Pattern.compile(
			"(?i)(?<![A-Za-z])(?:GMT|UTC)\\s*(?:[+-]\\s*\\d{1,2}(?::?\\d{2})?)?|\\bKST\\b");
	private static final Pattern ISO_OFFSET = Pattern.compile("(:\\d{2}(?:\\.\\d+)?)\\s*(?:Z|[+-]\\d{2}:?\\d{2})$");
	private static final Pattern ISO_T = Pattern.compile("(?<=\\d)T(?=\\d)");
	private static final Pattern KOREAN_UNITS = Pattern.compile("[년월일시분초]");
	private static final Pattern LETTER = Pattern.compile("\\p{L}");
	private static final Pattern NUMBER = Pattern.compile("\\d+");

	private FormTimestampParser() {
	}

	/**
	 * 연도가 뒤에 오는 형식(예: 05/11/2026)에서 일이 먼저인지 판단한다. 첫 숫자가 12를 넘는 값이
	 * 하나라도 있으면 일/월/연 순서, 아니면 구글폼 영어 로케일 기본값인 월/일/연 순서로 본다.
	 */
	public static boolean detectDayFirst(Collection<String> values) {
		boolean dayFirst = false;
		for (String value : values) {
			List<String> numbers = numbersOf(value);
			if (numbers == null || numbers.size() < 3 || numbers.get(0).length() == 4 || numbers.get(2).length() != 4) {
				continue;
			}
			int first = Integer.parseInt(numbers.get(0));
			int second = Integer.parseInt(numbers.get(1));
			if (second > 12) {
				return false;
			}
			if (first > 12) {
				dayFirst = true;
			}
		}
		return dayFirst;
	}

	public static Optional<LocalDateTime> parse(String raw) {
		return parse(raw, false);
	}

	public static Optional<LocalDateTime> parse(String raw, boolean dayFirst) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		String text = raw.strip();
		boolean am = AM.matcher(text).find();
		boolean pm = PM.matcher(text).find();
		if (am && pm) {
			return Optional.empty();
		}
		List<String> numbers = numbersOf(text);
		if (numbers == null || numbers.size() < 5 || numbers.size() > 7) {
			return Optional.empty();
		}
		for (String number : numbers) {
			if (number.length() > 4) {
				return Optional.empty();
			}
		}

		int year;
		int month;
		int day;
		if (numbers.get(0).length() == 4) {
			year = Integer.parseInt(numbers.get(0));
			month = Integer.parseInt(numbers.get(1));
			day = Integer.parseInt(numbers.get(2));
		} else if (numbers.get(2).length() == 4) {
			year = Integer.parseInt(numbers.get(2));
			int first = Integer.parseInt(numbers.get(0));
			int second = Integer.parseInt(numbers.get(1));
			month = dayFirst ? second : first;
			day = dayFirst ? first : second;
		} else {
			return Optional.empty();
		}

		int hour = Integer.parseInt(numbers.get(3));
		int minute = Integer.parseInt(numbers.get(4));
		// 7번째 숫자는 소수점 아래 초다. 버린다.
		int second = numbers.size() >= 6 ? Integer.parseInt(numbers.get(5)) : 0;
		if (am || pm) {
			if (hour < 1 || hour > 12) {
				return Optional.empty();
			}
			hour = hour % 12 + (pm ? 12 : 0);
		}
		try {
			return Optional.of(LocalDateTime.of(year, month, day, hour, minute, second));
		} catch (DateTimeException e) {
			return Optional.empty();
		}
	}

	/** 오전/오후·시간대 표시를 지운 뒤 숫자 덩어리를 뽑는다. 해석할 수 없는 글자가 남으면 null. */
	private static List<String> numbersOf(String raw) {
		if (raw == null) {
			return null;
		}
		String text = raw.strip();
		text = AM.matcher(text).replaceAll(" ");
		text = PM.matcher(text).replaceAll(" ");
		text = NAMED_ZONE.matcher(text).replaceAll(" ");
		text = ISO_OFFSET.matcher(text).replaceAll("$1");
		text = ISO_T.matcher(text).replaceAll(" ");
		text = KOREAN_UNITS.matcher(text).replaceAll(" ");
		if (LETTER.matcher(text).find()) {
			return null;
		}
		List<String> numbers = new ArrayList<>();
		Matcher matcher = NUMBER.matcher(text);
		while (matcher.find()) {
			numbers.add(matcher.group());
		}
		return numbers;
	}
}
