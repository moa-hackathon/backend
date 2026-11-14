package com.moa.backend.notice.service;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.discord.application.port.in.SendDiscordMessageUseCase;
import com.moa.backend.discord.domain.DiscordSendResult;
import com.moa.backend.discord.domain.DiscordWebhook;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import com.moa.backend.notice.domain.FinalNotice;
import com.moa.backend.notice.dto.FinalNoticeResponse;
import com.moa.backend.notice.dto.NoticeSendResponse;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * 최종 공지 조회와 디스코드 발송.
 * 디스코드 호출 동안 DB 트랜잭션을 잡고 있지 않도록, 발송 성공 뒤 짧은 트랜잭션에서 발송 시각만 저장한다.
 * 웹훅 URL에는 토큰이 들어 있으므로 응답, 로그, 오류 메시지 어디에도 넣지 않는다.
 */
@Slf4j
@Service
public class NoticeSendService {

	static final String CHANNEL_DISCORD = "DISCORD";

	private final EventRepository eventRepository;
	private final SendDiscordMessageUseCase sendDiscordMessageUseCase;
	private final TransactionTemplate transactionTemplate;

	public NoticeSendService(EventRepository eventRepository, SendDiscordMessageUseCase sendDiscordMessageUseCase,
			PlatformTransactionManager transactionManager) {
		this.eventRepository = eventRepository;
		this.sendDiscordMessageUseCase = sendDiscordMessageUseCase;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public FinalNoticeResponse getFinalNotice(Club club, Long eventId) {
		Event event = findOwnedEvent(club, eventId);
		FinalNotice notice = buildFinalNotice(event);
		return new FinalNoticeResponse(event.getId(), notice.text(), blankToNull(event.getFormUrl()),
				notice.formLinkMissing(), club.isDiscordConnected(), event.getNoticeSentAt());
	}

	public NoticeSendResponse sendToDiscord(Club club, Long eventId, LocalDateTime now) {
		Event event = findOwnedEvent(club, eventId);
		FinalNotice notice = buildFinalNotice(event);
		if (notice.formLinkMissing()) {
			throw new ApiException(ErrorCode.NOTICE_NOT_READY, "구글폼 링크를 먼저 연결해 주세요.");
		}
		if (!club.isDiscordConnected()) {
			throw new ApiException(ErrorCode.DISCORD_NOT_CONNECTED);
		}
		String webhookUrl = club.getDiscordWebhookUrl();
		validateWebhook(webhookUrl);

		DiscordSendResult result = send(webhookUrl, notice.text(), eventId);

		transactionTemplate.executeWithoutResult(status -> eventRepository.findById(eventId)
				.orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND))
				.markNoticeSent(now));
		return new NoticeSendResponse(eventId, CHANNEL_DISCORD, now, result.messageCount());
	}

	private DiscordSendResult send(String webhookUrl, String content, Long eventId) {
		try {
			return sendDiscordMessageUseCase.send(webhookUrl, content);
		} catch (IllegalArgumentException e) {
			// 웹훅 형식은 미리 확인했으므로 여기서는 글 길이 초과다.
			throw new ApiException(ErrorCode.INVALID_REQUEST, "공지가 너무 길어 디스코드로 보낼 수 없습니다.");
		} catch (IllegalStateException e) {
			// 원인 예외에는 호출 URL(토큰 포함)이 들어 있을 수 있어 스택은 남기지 않는다.
			log.warn("공지 디스코드 발송 실패 eventId={}: {}", eventId, e.getMessage());
			throw new ApiException(ErrorCode.DISCORD_SEND_FAILED);
		}
	}

	private void validateWebhook(String webhookUrl) {
		try {
			DiscordWebhook.parse(webhookUrl);
		} catch (IllegalArgumentException e) {
			throw new ApiException(ErrorCode.DISCORD_WEBHOOK_INVALID);
		}
	}

	private Event findOwnedEvent(Club club, Long eventId) {
		return eventRepository.findByIdAndClubId(eventId, club.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND));
	}

	private FinalNotice buildFinalNotice(Event event) {
		if (!StringUtils.hasText(event.getNoticeText())) {
			throw new ApiException(ErrorCode.NOTICE_NOT_READY);
		}
		return FinalNotice.of(event.getNoticeText(), event.getFormUrl());
	}

	private static String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.strip() : null;
	}
}
