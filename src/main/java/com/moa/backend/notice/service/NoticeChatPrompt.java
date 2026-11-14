package com.moa.backend.notice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moa.backend.notice.dto.NoticeChatMessage;
import com.moa.backend.notice.dto.NoticeChatSummary;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * 공지 입력 채팅의 프롬프트를 만든다. LLM은 한 번 묻고 한 번 답하는 텍스트 API라서
 * 대화 전체와 현재 요약을 사용자 프롬프트에 넣고, 답은 JSON 하나로 받는다.
 */
class NoticeChatPrompt {

	/** 달력에 넣을 주 수 (이번 주 포함). 그 뒤 날짜는 모델이 계산한다. */
	static final int CALENDAR_WEEKS = 6;

	static final String RETRY_INSTRUCTION =
			"\n\n주의: 직전 답변은 올바른 JSON이 아니었다. 설명이나 코드블록 없이 JSON 객체 하나만 출력해.";

	private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

	private static final String SYSTEM_TEMPLATE = """
			너는 동아리 운영진이 행사 공지를 만들도록 돕는 도우미다.
			운영진이 편하게 적은 행사 내용을 행사 요약으로 정리하고, 빠진 필수 항목을 짧게 되묻는다.

			[오늘]
			%s, 시간대 Asia/Seoul

			[달력] 한 주는 월요일에 시작한다.
			%s

			[항목]
			- title: 행사명. 운영진이 말한 행사 종류로 짧게 짓는다 (예: "네트워킹 행사" → "%s 네트워킹 행사"). 행사가 무엇인지 모르면 null.
			- eventAt: 행사 일시, "yyyy-MM-ddTHH:mm".
			- location: 장소.
			- capacity: 정원 (정수, 명).
			- minCapacity: 최소 필수 인원 (정수, 명). 예: "20명은 넘어야 진행해요" → 20.
			- applyDeadline: 신청 마감 일시, "yyyy-MM-ddTHH:mm". 날짜만 있으면 그날 23:59.
			- extraQuestions: 신청서에 추가로 물어볼 질문. 운영진이 받겠다고 한 것만 짧은 명사구로 (예: "뒤풀이 참석 여부").
			- description: 음식, 준비물, 회비 같은 기타 안내. 없으면 null.

			[규칙]
			1. 운영진이 말하지 않은 값은 절대 지어내지 않는다. 모르면 null로 둔다.
			2. 상대 날짜는 오늘과 달력으로 정한다. "이번 주"는 오늘이 속한 주, "다음 주"는 그다음 주다. 연도가 없으면 오늘 이후 가장 가까운 날짜다.
			3. "저녁 7시", "오후 7시"는 19:00이다. "아침", "오전"이 없고 1~11시라고만 하면 오후로 본다.
			4. 행사 일시는 날짜와 시각이 모두 있어야 한다. 시각을 모르면 eventAt은 null이다.
			5. 현재 요약에 있는 값은 운영진이 바꾸지 않는 한 그대로 둔다. 운영진이 고친 값은 새 값으로 바꾼다.
			6. 운영진이 고른 추가 질문은 extraQuestions에 넣는다.
			7. suggestedExtraQuestions: 이 행사에서 신청받을 때 물어보면 좋을 질문 후보 2~4개를 짧은 명사구로 (예: "뒤풀이 참석 여부", "알레르기"). extraQuestions에 이미 있는 것은 뺀다.
			8. reply: 해요체 한국어 1~2문장. 빠진 필수 항목(title, eventAt, location, capacity, minCapacity, applyDeadline)이 있으면 그 항목을 자연스럽게 묻는다. 다 채워졌으면 "이렇게 정리했어요."처럼 짧게 마무리한다.

			[출력]
			설명이나 코드블록 없이 아래 형식의 JSON 객체 하나만 출력한다.
			{"reply": "...", "summary": {"title": null, "eventAt": null, "location": null, "capacity": null, "minCapacity": null, "applyDeadline": null, "extraQuestions": [], "description": null}, "suggestedExtraQuestions": []}
			""";

	private final ObjectMapper objectMapper;

	NoticeChatPrompt(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	String system(LocalDate today, String clubName) {
		return SYSTEM_TEMPLATE.formatted(formatDay(today), calendar(today), clubName);
	}

	String user(String clubName, NoticeChatSummary summary, List<String> selectedExtraQuestions,
			List<NoticeChatMessage> messages) {
		ArrayNode conversation = objectMapper.createArrayNode();
		for (NoticeChatMessage message : messages) {
			conversation.addObject()
					.put("role", message.role() == null ? "user" : message.role().name().toLowerCase(Locale.ROOT))
					.put("content", message.content());
		}
		return """
				동아리: %s

				현재 요약:
				%s

				운영진이 고른 추가 질문:
				%s

				지금까지의 대화 (user는 운영진, assistant는 너):
				%s

				마지막 운영진 메시지까지 반영해 JSON을 출력해.""".formatted(
				clubName,
				toJson(summaryNode(summary)),
				toJson(objectMapper.valueToTree(selectedExtraQuestions == null ? List.of() : selectedExtraQuestions)),
				toJson(conversation));
	}

	static String calendar(LocalDate today) {
		LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		StringBuilder sb = new StringBuilder();
		for (int week = 0; week < CALENDAR_WEEKS; week++) {
			String name = switch (week) {
				case 0 -> "이번 주";
				case 1 -> "다음 주";
				case 2 -> "다다음 주";
				default -> week + "주 뒤";
			};
			sb.append("- ").append(name).append(": ");
			for (int day = 0; day < 7; day++) {
				if (day > 0) {
					sb.append(", ");
				}
				sb.append(formatDay(monday.plusWeeks(week).plusDays(day)));
			}
			sb.append('\n');
		}
		return sb.toString().stripTrailing();
	}

	static String formatDay(LocalDate date) {
		return date + "(" + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")";
	}

	private ObjectNode summaryNode(NoticeChatSummary summary) {
		NoticeChatSummary s = summary == null ? NoticeChatSummary.empty() : summary;
		ObjectNode node = objectMapper.createObjectNode();
		node.put("title", s.title());
		node.put("eventAt", s.eventAt() == null ? null : formatDateTime(s.eventAt()));
		node.put("location", s.location());
		node.put("capacity", s.capacity());
		node.put("minCapacity", s.minCapacity());
		node.put("applyDeadline", s.applyDeadline() == null ? null : formatDateTime(s.applyDeadline()));
		node.set("extraQuestions", objectMapper.valueToTree(s.extraQuestions()));
		node.put("description", s.description());
		return node;
	}

	private static String formatDateTime(LocalDateTime dateTime) {
		return dateTime.format(ISO_DATE_TIME);
	}

	private String toJson(Object node) {
		try {
			return objectMapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("프롬프트를 만들지 못했습니다.", e);
		}
	}
}
