package com.moa.backend.notice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.backend.notice.dto.NoticeChatSummary;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 답변 텍스트를 읽는다. 모델은 JSON 모드가 없어서 코드블록이나 앞뒤 설명을 붙이기도 하므로
 * 첫 '{'부터 마지막 '}'까지만 잘라 읽고, 값마다 타입과 범위를 확인해 맞지 않으면 null로 둔다.
 */
class NoticeChatResponseParser {

	static final int MAX_PEOPLE = 100_000;
	static final int MAX_REPLY_LENGTH = 500;
	static final int MAX_SUGGESTIONS = 4;

	private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);
	private static final Pattern PEOPLE = Pattern.compile("^\\D*?(\\d{1,6})\\s*(명|인)?\\D*$");

	private final ObjectMapper objectMapper;

	NoticeChatResponseParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/** JSON 객체로 읽히지 않으면 비어 있다. */
	Optional<NoticeChatModelOutput> parse(String text) {
		Optional<JsonNode> root = readObject(text);
		if (root.isEmpty()) {
			return Optional.empty();
		}
		JsonNode node = root.get();
		JsonNode summary = node.path("summary").isObject() ? node.get("summary") : node;
		return Optional.of(new NoticeChatModelOutput(
				text(node.get("reply"), MAX_REPLY_LENGTH),
				text(summary.get("title"), NoticeChatSummary.MAX_TITLE_LENGTH),
				dateTime(summary.get("eventAt"), false),
				text(summary.get("location"), NoticeChatSummary.MAX_LOCATION_LENGTH),
				people(summary.get("capacity")),
				people(summary.get("minCapacity")),
				dateTime(summary.get("applyDeadline"), true),
				textList(summary.get("extraQuestions"), NoticeChatSummary.MAX_EXTRA_QUESTIONS),
				text(summary.get("description"), NoticeChatSummary.MAX_DESCRIPTION_LENGTH),
				textList(node.get("suggestedExtraQuestions"), MAX_SUGGESTIONS)));
	}

	private Optional<JsonNode> readObject(String text) {
		if (text == null) {
			return Optional.empty();
		}
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start < 0 || end <= start) {
			return Optional.empty();
		}
		try {
			JsonNode node = objectMapper.readTree(text.substring(start, end + 1));
			return node != null && node.isObject() ? Optional.of(node) : Optional.empty();
		} catch (JsonProcessingException e) {
			return Optional.empty();
		}
	}

	static String text(JsonNode node, int maxLength) {
		if (node == null || !node.isValueNode() || node.isNull()) {
			return null;
		}
		return cleanText(node.asText(), maxLength);
	}

	static String cleanText(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		String trimmed = value.strip();
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("null")) {
			return null;
		}
		return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
	}

	static Integer people(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		Long value = null;
		if (node.isIntegralNumber()) {
			value = node.asLong();
		} else if (node.isNumber() && node.asDouble() == Math.rint(node.asDouble())) {
			value = (long) node.asDouble();
		} else if (node.isTextual()) {
			Matcher matcher = PEOPLE.matcher(node.asText().strip());
			if (matcher.matches()) {
				value = Long.parseLong(matcher.group(1));
			}
		}
		return validPeople(value);
	}

	static Integer validPeople(Number value) {
		if (value == null || value.longValue() <= 0 || value.longValue() > MAX_PEOPLE) {
			return null;
		}
		return value.intValue();
	}

	/**
	 * @param dateOnlyMeansEndOfDay true면 날짜만 있을 때 그날 23:59로 본다 (신청 마감일).
	 *                              false면 시각을 모르는 것이므로 null이다 (행사 일시).
	 */
	static LocalDateTime dateTime(JsonNode node, boolean dateOnlyMeansEndOfDay) {
		if (node == null || !node.isTextual()) {
			return null;
		}
		String value = node.asText().strip().replace(' ', 'T');
		if (value.isEmpty()) {
			return null;
		}
		try {
			return LocalDateTime.parse(value).truncatedTo(ChronoUnit.MINUTES);
		} catch (DateTimeParseException ignored) {
			// 날짜만 왔을 수 있다
		}
		try {
			LocalDate date = LocalDate.parse(value);
			return dateOnlyMeansEndOfDay ? date.atTime(END_OF_DAY) : null;
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	static List<String> textList(JsonNode node, int maxSize) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (!node.isArray()) {
			return null;
		}
		List<String> values = new ArrayList<>();
		for (JsonNode item : node) {
			values.add(item.isValueNode() ? item.asText() : null);
		}
		return cleanList(values, maxSize);
	}

	/** 앞뒤 공백을 지우고 빈 값과 중복을 뺀다. */
	static List<String> cleanList(List<String> values, int maxSize) {
		if (values == null) {
			return List.of();
		}
		Set<String> cleaned = new LinkedHashSet<>();
		for (String value : values) {
			String text = cleanText(value, NoticeChatSummary.MAX_EXTRA_QUESTION_LENGTH);
			if (text != null && cleaned.size() < maxSize) {
				cleaned.add(text);
			}
		}
		return List.copyOf(cleaned);
	}
}
