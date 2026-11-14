package com.moa.backend.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.discord.application.port.in.SendDiscordMessageUseCase;
import com.moa.backend.discord.domain.DiscordSendResult;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import com.moa.backend.notice.dto.FinalNoticeResponse;
import com.moa.backend.notice.dto.NoticeSendResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class NoticeSendServiceTest {

	private static final String WEBHOOK = "https://discord.com/api/webhooks/123/secret-token";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 13, 0);
	private static final String NOTICE = "[모아] 네트워킹 행사 안내\n신청 링크: [구글폼 링크]";
	private static final String FORM_URL = "https://forms.gle/abc";

	@Mock
	private EventRepository eventRepository;

	@Mock
	private SendDiscordMessageUseCase sendDiscordMessageUseCase;

	@Mock
	private PlatformTransactionManager transactionManager;

	private NoticeSendService service;

	@BeforeEach
	void setUp() {
		service = new NoticeSendService(eventRepository, sendDiscordMessageUseCase, transactionManager);
	}

	@Test
	void 최종_공지는_링크를_넣어_돌려준다() {
		Club club = club(WEBHOOK);
		Event event = event(club, NOTICE, FORM_URL);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event));

		FinalNoticeResponse response = service.getFinalNotice(club, 10L);

		assertThat(response.eventId()).isEqualTo(10L);
		assertThat(response.noticeText()).isEqualTo("[모아] 네트워킹 행사 안내\n신청 링크: https://forms.gle/abc");
		assertThat(response.formUrl()).isEqualTo(FORM_URL);
		assertThat(response.formLinkMissing()).isFalse();
		assertThat(response.discordConnected()).isTrue();
		assertThat(response.sentAt()).isNull();
	}

	@Test
	void 링크가_없으면_자리_표시와_함께_알려준다() {
		Club club = club(null);
		Event event = event(club, NOTICE, null);
		event.markNoticeSent(NOW);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event));

		FinalNoticeResponse response = service.getFinalNotice(club, 10L);

		assertThat(response.noticeText()).isEqualTo(NOTICE);
		assertThat(response.formUrl()).isNull();
		assertThat(response.formLinkMissing()).isTrue();
		assertThat(response.discordConnected()).isFalse();
		assertThat(response.sentAt()).isEqualTo(NOW);
	}

	@Test
	void 다른_동아리_행사는_찾을_수_없다() {
		Club club = club(WEBHOOK);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.empty());

		assertError(() -> service.getFinalNotice(club, 10L), ErrorCode.EVENT_NOT_FOUND);
		assertError(() -> service.sendToDiscord(club, 10L, NOW), ErrorCode.EVENT_NOT_FOUND);
		verifyNoInteractions(sendDiscordMessageUseCase);
	}

	@Test
	void 공지_본문이_없으면_준비되지_않았다() {
		Club club = club(WEBHOOK);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event(club, " ", FORM_URL)));

		assertError(() -> service.getFinalNotice(club, 10L), ErrorCode.NOTICE_NOT_READY);
		assertError(() -> service.sendToDiscord(club, 10L, NOW), ErrorCode.NOTICE_NOT_READY);
		verifyNoInteractions(sendDiscordMessageUseCase);
	}

	@Test
	void 디스코드로_보내고_발송_시각을_저장한다() {
		Club club = club(WEBHOOK);
		Event event = event(club, NOTICE, FORM_URL);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event));
		given(eventRepository.findById(10L)).willReturn(Optional.of(event));
		given(sendDiscordMessageUseCase.send(WEBHOOK, "[모아] 네트워킹 행사 안내\n신청 링크: https://forms.gle/abc"))
				.willReturn(new DiscordSendResult(1, List.of("m1")));

		NoticeSendResponse response = service.sendToDiscord(club, 10L, NOW);

		assertThat(response).isEqualTo(new NoticeSendResponse(10L, "DISCORD", NOW, 1));
		assertThat(event.getNoticeSentAt()).isEqualTo(NOW);
		verify(transactionManager).commit(any());
	}

	@Test
	void 다시_보내면_발송_시각이_갱신된다() {
		Club club = club(WEBHOOK);
		Event event = event(club, NOTICE, FORM_URL);
		event.markNoticeSent(NOW.minusDays(1));
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event));
		given(eventRepository.findById(10L)).willReturn(Optional.of(event));
		given(sendDiscordMessageUseCase.send(anyString(), anyString()))
				.willReturn(new DiscordSendResult(2, List.of("m1", "m2")));

		NoticeSendResponse response = service.sendToDiscord(club, 10L, NOW);

		assertThat(response.messageCount()).isEqualTo(2);
		assertThat(event.getNoticeSentAt()).isEqualTo(NOW);
	}

	@Test
	void 구글폼_링크가_없으면_보내지_않는다() {
		Club club = club(WEBHOOK);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event(club, NOTICE, null)));

		assertError(() -> service.sendToDiscord(club, 10L, NOW), ErrorCode.NOTICE_NOT_READY);
		verifyNoInteractions(sendDiscordMessageUseCase);
	}

	@Test
	void 웹훅이_없으면_보내지_않는다() {
		Club club = club(null);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event(club, NOTICE, FORM_URL)));

		assertError(() -> service.sendToDiscord(club, 10L, NOW), ErrorCode.DISCORD_NOT_CONNECTED);
		verifyNoInteractions(sendDiscordMessageUseCase);
	}

	@Test
	void 저장된_웹훅_형식이_틀리면_보내지_않는다() {
		Club club = club("https://evil.example.com/hook?token=secret-token");
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event(club, NOTICE, FORM_URL)));

		ApiException e = assertError(() -> service.sendToDiscord(club, 10L, NOW), ErrorCode.DISCORD_WEBHOOK_INVALID);
		assertThat(e.getMessage()).doesNotContain("secret-token");
		verifyNoInteractions(sendDiscordMessageUseCase);
	}

	@Test
	void 발송에_실패하면_발송_시각을_저장하지_않는다() {
		Club club = club(WEBHOOK);
		Event event = event(club, NOTICE, FORM_URL);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event));
		given(sendDiscordMessageUseCase.send(anyString(), anyString()))
				.willThrow(new IllegalStateException("Discord Webhook 오류 (HTTP 404)"));

		ApiException e = assertError(() -> service.sendToDiscord(club, 10L, NOW), ErrorCode.DISCORD_SEND_FAILED);

		assertThat(e.getMessage()).doesNotContain("secret-token");
		assertThat(event.getNoticeSentAt()).isNull();
		verify(eventRepository, never()).findById(any());
		verifyNoInteractions(transactionManager);
	}

	@Test
	void 공지가_너무_길면_잘못된_요청() {
		Club club = club(WEBHOOK);
		Event event = event(club, NOTICE, FORM_URL);
		given(eventRepository.findByIdAndClubId(10L, 1L)).willReturn(Optional.of(event));
		given(sendDiscordMessageUseCase.send(anyString(), anyString()))
				.willThrow(new IllegalArgumentException("글이 너무 깁니다."));

		assertError(() -> service.sendToDiscord(club, 10L, NOW), ErrorCode.INVALID_REQUEST);
		assertThat(event.getNoticeSentAt()).isNull();
	}

	private static ApiException assertError(Runnable action, ErrorCode errorCode) {
		try {
			action.run();
		} catch (ApiException e) {
			assertThat(e.getErrorCode()).isEqualTo(errorCode);
			return e;
		}
		throw new AssertionError("ApiException(" + errorCode + ")이 발생하지 않았습니다.");
	}

	private static Club club(String webhookUrl) {
		Club club = Club.builder().name("모아").inviteCode("code").discordWebhookUrl(webhookUrl).build();
		ReflectionTestUtils.setField(club, "id", 1L);
		return club;
	}

	private static Event event(Club club, String noticeText, String formUrl) {
		Event event = Event.builder()
				.club(club)
				.title("모아 네트워킹 행사")
				.location("공학관 302호")
				.eventAt(LocalDateTime.of(2026, 11, 21, 19, 0))
				.capacity(40)
				.minCapacity(20)
				.applyDeadline(LocalDateTime.of(2026, 11, 19, 23, 59))
				.shareCode("share")
				.noticeText(noticeText)
				.formUrl(formUrl)
				.build();
		ReflectionTestUtils.setField(event, "id", 10L);
		return event;
	}
}
