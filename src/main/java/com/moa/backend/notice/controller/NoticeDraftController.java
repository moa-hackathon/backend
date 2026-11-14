package com.moa.backend.notice.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.notice.dto.NoticeDraftRequest;
import com.moa.backend.notice.dto.NoticeDraftResponse;
import com.moa.backend.notice.service.NoticeDraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공지", description = "운영진용 공지 작성 API")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeDraftController {

	private final NoticeDraftService noticeDraftService;

	@Operation(
			summary = "공지 초안 생성 / 수정 요청 (F2)",
			description = "완성된 행사 요약으로 공지 초안을 만듭니다. 신청 링크 자리는 [구글폼 링크]로 남깁니다. "
					+ "previousDraft와 revisionRequest를 함께 보내면 이전 초안을 수정 요청대로 고칩니다. "
					+ "처음 만들 때 AI 응답을 받지 못하면 기본 양식(source=TEMPLATE)으로 만들고, "
					+ "수정 요청이 실패하면 502(AI_RESPONSE_FAILED)를 돌려줍니다.")
	@PostMapping("/draft")
	public NoticeDraftResponse draft(@CurrentClub Club club, @RequestBody NoticeDraftRequest request) {
		return noticeDraftService.draft(club, request);
	}
}
