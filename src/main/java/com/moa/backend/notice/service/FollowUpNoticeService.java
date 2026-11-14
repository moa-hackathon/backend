package com.moa.backend.notice.service;

import com.moa.backend.ai.application.port.in.GenerateTextUseCase;
import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.application.repository.ApplicationRepository;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.dashboard.domain.ApplicationReview;
import com.moa.backend.dashboard.domain.NextAction;
import com.moa.backend.event.domain.EventProgress;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import com.moa.backend.notice.domain.FormLinkPlaceholder;
import com.moa.backend.notice.dto.FinalNoticeResponse;
import com.moa.backend.notice.dto.FollowUpNoticeDraftResponse;
import com.moa.backend.notice.dto.NoticeDraftSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 "다음 할 일"(F11)의 후속 공지. 기존 행사의 현재 신청 현황으로 초안을 만들고, 확정한 공지를 행사에 저장한다.
 * 사실 줄(일시, 장소, 현황)은 코드에서 만들고 AI는 첫머리 안내 문장만 쓴다. AI를 쓸 수 없으면 기본 문장을 쓴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpNoticeService {

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("M/d(E) HH:mm", Locale.KOREAN);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d(E)", Locale.KOREAN);
	private static final int LEAD_MAX_LENGTH = 300;

	private static final String SYSTEM_PROMPT = """
			너는 대학 동아리 운영진을 돕는 공지 작성 도우미다.
			주어진 상황에 맞는 후속 공지의 첫머리 안내 문장을 한국어로 1~2문장만 쓴다.
			주어진 숫자와 사실만 쓰고 새로운 사실, 날짜, 링크를 지어내지 않는다.
			제목, 목록, 따옴표, 마크다운 없이 문장만 출력한다.""";

	private final EventRepository eventRepository;
	private final ApplicationRepository applicationRepository;
	private final GenerateTextUseCase generateTextUseCase;

	@Transactional(readOnly = true)
	public FollowUpNoticeDraftResponse draft(Club club, Long eventId, NextAction.Type requestedType, LocalDateTime now) {
		Event event = findOwnedEvent(club, eventId);
		int applicantCount = ApplicationReview.of(
				applicationRepository.findAllByEventIdOrderByAppliedAtAscIdAsc(event.getId()),
				event.getFormQuestions()).applicantCount();
		EventProgress progress = EventProgress.of(applicantCount, event.getCapacity(), event.getMinCapacity(),
				event.getApplyDeadline(), now);
		NextAction.Type type = requestedType != null ? requestedType : NextAction.from(progress).type();

		String aiLead = generateLead(event, progress, type);
		String lead = aiLead != null ? aiLead : defaultLead(progress, type);
		String noticeText = render(club, event, progress, type, lead);
		return new FollowUpNoticeDraftResponse(type, noticeText,
				aiLead != null ? NoticeDraftSource.AI : NoticeDraftSource.TEMPLATE);
	}

	@Transactional
	public FinalNoticeResponse updateNotice(Club club, Long eventId, String noticeText) {
		Event event = findOwnedEvent(club, eventId);
		event.updateNotice(FormLinkPlaceholder.ensure(noticeText.strip()));
		String formUrl = event.getFormUrl();
		boolean formLinkMissing = formUrl == null || formUrl.isBlank();
		String finalText = formLinkMissing
				? event.getNoticeText()
				: event.getNoticeText().replace(FormLinkPlaceholder.VALUE, formUrl);
		return new FinalNoticeResponse(event.getId(), finalText, formLinkMissing ? null : formUrl, formLinkMissing,
				club.isDiscordConnected(), event.getNoticeSentAt());
	}

	private Event findOwnedEvent(Club club, Long eventId) {
		return eventRepository.findByIdAndClubId(eventId, club.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND));
	}

	private String render(Club club, Event event, EventProgress progress, NextAction.Type type, String lead) {
		List<String> lines = new ArrayList<>();
		lines.add("[" + club.getName() + "] " + event.getTitle() + " " + headingSuffix(type));
		lines.add(lead);
		lines.add("일시: " + DATE_TIME.format(event.getEventAt()));
		lines.add("장소: " + event.getLocation());
		lines.add("신청 현황: " + progress.applicantCount() + " / " + progress.capacity() + "명");
		if (type != NextAction.Type.ANNOUNCE_CLOSED) {
			lines.add("신청 마감: " + DATE.format(event.getApplyDeadline()));
			lines.add(FormLinkPlaceholder.LINE);
		}
		return String.join("\n", lines);
	}

	private static String headingSuffix(NextAction.Type type) {
		return switch (type) {
			case RECRUIT_MORE -> "추가 모집";
			case ENCOURAGE -> "신청 안내";
			case CLOSING_SOON -> "마감 임박";
			case ANNOUNCE_CLOSED -> "신청 마감 안내";
		};
	}

	private static String defaultLead(EventProgress progress, NextAction.Type type) {
		return switch (type) {
			case RECRUIT_MORE -> "행사 진행까지 %d명이 더 필요해요. 아직 신청하지 않았다면 함께해 주세요!"
					.formatted(progress.remainingToMin());
			case ENCOURAGE -> "아직 자리가 %d명 남아 있어요. 관심 있는 분들은 신청해 주세요!"
					.formatted(progress.remainingToCapacity());
			case CLOSING_SOON -> "정원까지 %d명 남았어요. 참여를 원하면 서둘러 신청해 주세요!"
					.formatted(progress.remainingToCapacity());
			case ANNOUNCE_CLOSED -> "정원이 모두 차서 신청을 마감합니다. 신청해 주신 분들 감사합니다!";
		};
	}

	/** AI가 쓴 첫머리 문장. 호출 실패, 빈 응답, 잘린 응답, 너무 길거나 링크가 섞인 응답이면 null. */
	private String generateLead(Event event, EventProgress progress, NextAction.Type type) {
		String userPrompt = """
				행사명: %s
				공지 종류: %s
				현재 신청 인원: %d명
				정원: %d명
				최소 필수 인원: %d명
				최소 인원까지 남은 인원: %d명
				정원까지 남은 인원: %d명"""
				.formatted(event.getTitle(), headingSuffix(type), progress.applicantCount(), progress.capacity(),
						progress.minCapacity(), progress.remainingToMin(), progress.remainingToCapacity());
		try {
			AiCompletion completion = generateTextUseCase.generate(null, SYSTEM_PROMPT, userPrompt);
			if (completion == null || completion.text() == null || completion.truncated()) {
				return null;
			}
			String text = completion.text().strip().replaceAll("\\s*\\n+\\s*", " ");
			if (text.isEmpty() || text.length() > LEAD_MAX_LENGTH || text.contains("http") || text.contains("```")) {
				return null;
			}
			return text;
		} catch (RuntimeException e) {
			log.warn("후속 공지 AI 호출 실패: {}", e.getMessage());
			return null;
		}
	}
}
