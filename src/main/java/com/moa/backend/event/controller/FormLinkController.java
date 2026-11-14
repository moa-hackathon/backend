package com.moa.backend.event.controller;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.auth.CurrentClub;
import com.moa.backend.event.dto.FormLinkRequest;
import com.moa.backend.event.dto.FormLinkResponse;
import com.moa.backend.event.dto.FormQuestionsResponse;
import com.moa.backend.event.service.FormLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "구글폼 연결", description = "06 구글폼 연결 화면 API")
@RestController
@RequestMapping("/api/events/{eventId}")
@RequiredArgsConstructor
public class FormLinkController {

	private final FormLinkService formLinkService;

	@Operation(
			summary = "구글폼 링크 확인 (링크 확인)",
			description = "forms.gle 또는 docs.google.com/forms 응답자용 링크를 받아, 로그인 없이 열리는 공개 폼인지 확인하고 "
					+ "응답 항목(문항 제목, 필수 여부)을 읽어 돌려줍니다. 확인되면 링크를 행사에 저장합니다(공지의 [구글폼 링크] 자리). "
					+ "형식이 틀리거나, 열 수 없거나, 로그인이 필요하거나, 응답을 받지 않는 폼이면 400 FORM_LINK_INVALID와 안내 문구를 돌려줍니다.")
	@PutMapping("/form-link")
	public FormLinkResponse connectForm(@CurrentClub Club club, @PathVariable Long eventId,
			@Valid @RequestBody FormLinkRequest request) {
		return formLinkService.connectForm(club, eventId, request.formUrl());
	}

	@Operation(
			summary = "폼 문항 추천 (아직 없어요)",
			description = "구글폼이 아직 없을 때 만들 문항을 추천합니다. 이름 / 학번 / 연락처는 필수, "
					+ "행사를 만들 때 입력한 추가 질문은 선택 문항으로 붙입니다. '문항 복사하기'용 글(copyText)도 함께 돌려줍니다.")
	@GetMapping("/form-questions")
	public FormQuestionsResponse recommendQuestions(@CurrentClub Club club, @PathVariable Long eventId) {
		return formLinkService.recommendQuestions(club, eventId);
	}
}
