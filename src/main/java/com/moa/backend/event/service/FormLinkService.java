package com.moa.backend.event.service;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.dto.FormLinkResponse;
import com.moa.backend.event.dto.FormQuestionResponse;
import com.moa.backend.event.dto.FormQuestionsResponse;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import com.moa.backend.googleform.application.port.in.ReadPublicFormUseCase;
import com.moa.backend.googleform.domain.FormLinkException;
import com.moa.backend.googleform.domain.PublicForm;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/** 06 구글폼 연결 화면: 폼 링크 확인과 폼 문항 추천. */
@Service
@RequiredArgsConstructor
public class FormLinkService {

	/** 추천 문항 중 항상 필수로 넣는 기본 항목. */
	static final List<String> BASE_QUESTIONS = List.of("이름", "학번", "연락처");

	private final EventRepository eventRepository;
	private final ReadPublicFormUseCase readPublicFormUseCase;
	private final TransactionOperations transactionOperations;

	/**
	 * 폼 링크를 확인하고 행사에 저장한다.
	 * 외부 요청(최대 수십 초)을 하는 동안 DB 트랜잭션을 잡고 있지 않도록, 폼을 읽은 뒤에 짧은 트랜잭션으로 저장한다.
	 */
	public FormLinkResponse connectForm(Club club, Long eventId, String formUrl) {
		getOwnedEvent(club, eventId);

		PublicForm form;
		try {
			form = readPublicFormUseCase.read(formUrl);
		} catch (FormLinkException e) {
			throw new ApiException(ErrorCode.FORM_LINK_INVALID, e.getMessage());
		}
		String savedUrl = formUrl.trim();

		transactionOperations.executeWithoutResult(status -> getOwnedEvent(club, eventId).connectForm(savedUrl));

		List<FormQuestionResponse> questions = form.items().stream()
				.map(item -> new FormQuestionResponse(item.title(), item.required()))
				.toList();
		return FormLinkResponse.of(eventId, savedUrl, form.title(), questions);
	}

	/** 폼이 아직 없을 때 추천 문항. 이름 / 학번 / 연락처는 필수, 행사의 추가 질문은 선택. */
	public FormQuestionsResponse recommendQuestions(Club club, Long eventId) {
		Event event = getOwnedEvent(club, eventId);

		List<FormQuestionResponse> questions = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>(BASE_QUESTIONS);
		BASE_QUESTIONS.forEach(title -> questions.add(new FormQuestionResponse(title, true)));
		if (event.getExtraQuestions() != null) {
			for (String extra : event.getExtraQuestions()) {
				if (extra == null || extra.isBlank()) {
					continue;
				}
				String title = extra.strip();
				if (seen.add(title)) {
					questions.add(new FormQuestionResponse(title, false));
				}
			}
		}
		return FormQuestionsResponse.of(questions);
	}

	private Event getOwnedEvent(Club club, Long eventId) {
		return eventRepository.findByIdAndClubId(eventId, club.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND));
	}
}
