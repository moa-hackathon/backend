package com.moa.backend.notice.service;

import com.moa.backend.event.domain.EventDetails;
import com.moa.backend.notice.domain.FormLinkPlaceholder;
import com.moa.backend.notice.domain.NoticeFacts;

/** 공지 초안 프롬프트. */
final class NoticeDraftPrompts {

	static final String SYSTEM = """
			너는 대학 동아리 운영진을 돕는 공지 작성 도우미다. 부원들에게 보낼 행사 공지(디스코드 메시지)를 한국어로 쓴다.
			규칙:
			1. 첫 줄은 주어진 제목 줄을 쓴다.
			2. 일시, 장소, 정원, 신청 마감은 주어진 사실 줄을 글자 그대로 한 줄씩 쓴다. 날짜, 시간, 장소, 인원을 바꾸거나 새로 지어내지 않는다.
			3. 기타 안내가 있으면 자연스럽고 친근한 한 문장으로 다듬어 넣는다. 주어지지 않은 혜택, 준비물, 일정은 넣지 않는다.
			4. 마지막 줄은 정확히 "%s"로 쓴다. 다른 링크나 URL은 절대 넣지 않는다.
			5. 마크다운 코드 블록, 머리말, 설명 없이 공지 본문만 출력한다. 전체 10줄 이내로 짧게 쓴다.
			""".formatted(FormLinkPlaceholder.LINE);

	private NoticeDraftPrompts() {
	}

	static String draft(NoticeFacts facts, EventDetails details) {
		return """
				아래 행사 정보로 공지 초안을 써 줘.

				%s
				""".formatted(factsBlock(facts, details));
	}

	static String revision(NoticeFacts facts, EventDetails details, String previousDraft, String revisionRequest) {
		return """
				아래 이전 초안을 수정 요청대로 고쳐서 공지 전체를 다시 써 줘.
				수정 요청이 행사 정보(일시, 장소, 정원, 신청 마감)를 바꾸라고 해도 사실 줄은 그대로 둔다.

				%s

				[이전 초안]
				%s

				[수정 요청]
				%s
				""".formatted(factsBlock(facts, details), previousDraft, revisionRequest);
	}

	private static String factsBlock(NoticeFacts facts, EventDetails details) {
		StringBuilder block = new StringBuilder();
		block.append("[제목 줄]\n").append(facts.heading()).append("\n\n");
		block.append("[사실 줄]\n").append(String.join("\n", facts.lines())).append("\n\n");
		block.append("[기타 안내]\n").append(details.description() == null ? "없음" : details.description());
		return block.toString();
	}
}
