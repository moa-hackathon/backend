package com.moa.backend.notice.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.notice.dto.NoticeChatRequest;
import com.moa.backend.notice.dto.NoticeChatResponse;
import com.moa.backend.notice.service.NoticeChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공지", description = "운영진용 공지 작성 API")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeChatController {

	private final NoticeChatService noticeChatService;

	@Operation(
			summary = "공지 입력 · 되묻기 (AI 채팅)",
			description = "운영진이 편하게 적은 행사 내용을 AI가 행사 요약(행사명, 일시, 장소, 정원, 최소 필수 인원, "
					+ "신청 마감일, 추가 질문)으로 정리하고 빠진 항목을 되묻습니다. 서버는 대화를 저장하지 않으므로 "
					+ "지금까지의 대화 전체와 직전 응답의 요약, 고른 추가 질문을 보내 주세요. \"다음 주 금요일\" 같은 "
					+ "상대 날짜는 Asia/Seoul 오늘 기준으로 풉니다. complete가 true면 공지 초안 API를 호출하면 됩니다. "
					+ "AI 답변을 받지 못하면 502 AI_RESPONSE_FAILED입니다.")
	@PostMapping("/chat")
	public NoticeChatResponse chat(@CurrentClub Club club, @Valid @RequestBody NoticeChatRequest request) {
		return noticeChatService.chat(club, request);
	}
}
