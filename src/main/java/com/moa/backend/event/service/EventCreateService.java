package com.moa.backend.event.service;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.domain.EventDetails;
import com.moa.backend.event.dto.EventCreateRequest;
import com.moa.backend.event.dto.EventCreateResponse;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import com.moa.backend.notice.domain.FormLinkPlaceholder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventCreateService {

	static final int NOTICE_TEXT_MAX_LENGTH = 5000;

	private final EventRepository eventRepository;
	private final ShareCodeGenerator shareCodeGenerator;

	/**
	 * 행사 요약과 확정한 공지로 동아리의 행사를 만든다. 공지에 신청 링크 자리 {@code [구글폼 링크]}가 없으면 마지막 줄에 붙인다.
	 */
	@Transactional
	public EventCreateResponse create(Club club, EventCreateRequest request) {
		EventDetails details = request.toDetails();
		String noticeText = request.noticeText();
		if (noticeText == null || noticeText.isBlank()) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "공지 본문을 입력해 주세요.");
		}
		if (noticeText.length() > NOTICE_TEXT_MAX_LENGTH) {
			throw new ApiException(ErrorCode.INVALID_REQUEST,
					"공지 본문은 " + NOTICE_TEXT_MAX_LENGTH + "자 이하여야 합니다.");
		}

		Event event = Event.builder()
				.club(club)
				.title(details.title())
				.description(details.description())
				.location(details.location())
				.eventAt(details.eventAt())
				.capacity(details.capacity())
				.minCapacity(details.minCapacity())
				.applyDeadline(details.applyDeadline())
				.shareCode(shareCodeGenerator.generate())
				.noticeText(FormLinkPlaceholder.ensure(noticeText))
				.extraQuestions(details.extraQuestions())
				.build();
		return EventCreateResponse.from(eventRepository.save(event));
	}
}
