package com.moa.backend.notice.domain;

import com.moa.backend.event.domain.EventDetails;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 공지에 들어갈 사실(일시, 장소, 정원, 신청 마감)을 정해진 형식으로 적은 것. 템플릿 공지와 AI 프롬프트가 같은 문구를 쓰고,
 * AI 초안에 이 사실이 빠지지 않았는지도 이 문구로 확인한다.
 *
 * @param heading 첫 줄 제목 (예: [모아] 네트워킹 행사 안내)
 * @param eventAt 일시 (예: 11/21(금) 19:00)
 * @param location 장소
 * @param capacity 정원 (예: 40명 (선착순))
 * @param applyDeadline 신청 마감 (예: 11/19(수), 자정이 아니면 시각까지)
 * @param description 기타 안내, 없으면 null
 */
public record NoticeFacts(
		String heading,
		String eventAt,
		String location,
		String capacity,
		String applyDeadline,
		String description
) {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	public static NoticeFacts of(String clubName, EventDetails details) {
		return new NoticeFacts(
				heading(clubName, details.title()),
				date(details.eventAt()) + " " + TIME.format(details.eventAt()),
				details.location(),
				details.capacity() + "명 (선착순)",
				deadline(details.applyDeadline()),
				details.description());
	}

	/** 사실 줄. 공지 본문에 이 순서대로 들어간다. */
	public List<String> lines() {
		List<String> lines = new ArrayList<>();
		lines.add("일시: " + eventAt);
		lines.add("장소: " + location);
		lines.add("정원: " + capacity);
		lines.add("신청 마감: " + applyDeadline);
		return lines;
	}

	/** AI 없이 만드는 공지. 디자인의 공지 초안과 같은 모양이다. */
	public String renderTemplate() {
		List<String> lines = new ArrayList<>();
		lines.add(heading);
		lines.addAll(lines());
		if (description != null) {
			lines.add(description);
		}
		lines.add(FormLinkPlaceholder.LINE);
		return String.join("\n", lines);
	}

	/**
	 * 초안에 일시, 장소, 정원, 신청 마감이 모두 그대로 들어 있는지. 띄어쓰기 차이는 무시한다.
	 */
	public boolean coveredBy(String draft) {
		String compact = compact(draft);
		return compact.contains(compact(eventAt))
				&& compact.contains(compact(location))
				&& compact.contains(compact(capacity.substring(0, capacity.indexOf('명') + 1)))
				&& compact.contains(compact(applyDeadline));
	}

	private static String compact(String value) {
		return value.replaceAll("\\s+", "");
	}

	private static String heading(String clubName, String title) {
		if (clubName == null || clubName.isBlank()) {
			return title + " 안내";
		}
		String club = clubName.trim();
		String rest = title.startsWith(club) && title.length() > club.length()
				? title.substring(club.length()).trim()
				: title;
		return "[" + club + "] " + rest + " 안내";
	}

	private static String deadline(LocalDateTime applyDeadline) {
		LocalTime time = applyDeadline.toLocalTime();
		boolean endOfDay = time.equals(LocalTime.MIDNIGHT) || (time.getHour() == 23 && time.getMinute() == 59);
		return endOfDay ? date(applyDeadline) : date(applyDeadline) + " " + TIME.format(applyDeadline);
	}

	private static String date(LocalDateTime dateTime) {
		return DATE.format(dateTime) + "(" + dateTime.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN)
				+ ")";
	}
}
