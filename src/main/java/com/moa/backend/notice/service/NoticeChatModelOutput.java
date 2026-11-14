package com.moa.backend.notice.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LLM 답변을 읽어 정리한 값. 모델이 비워 둔 값은 null이고, 목록은 모델이 아예 주지 않았을 때만 null이다.
 */
record NoticeChatModelOutput(
		String reply,
		String title,
		LocalDateTime eventAt,
		String location,
		Integer capacity,
		Integer minCapacity,
		LocalDateTime applyDeadline,
		List<String> extraQuestions,
		String description,
		List<String> suggestedExtraQuestions) {
}
