package com.moa.backend.notice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class NoticeChatPromptTest {

	@Test
	void 달력은_오늘이_속한_주의_월요일부터_시작한다() {
		String calendar = NoticeChatPrompt.calendar(LocalDate.of(2026, 11, 14));

		assertThat(calendar.lines()).hasSize(NoticeChatPrompt.CALENDAR_WEEKS);
		assertThat(calendar.lines().findFirst().orElseThrow())
				.isEqualTo("- 이번 주: 2026-11-09(월), 2026-11-10(화), 2026-11-11(수), 2026-11-12(목), "
						+ "2026-11-13(금), 2026-11-14(토), 2026-11-15(일)");
		assertThat(calendar).contains("- 다음 주: 2026-11-16(월)").contains("2026-11-20(금)");
	}

	@Test
	void 월요일이면_그날부터_이번_주다() {
		String calendar = NoticeChatPrompt.calendar(LocalDate.of(2026, 11, 16));

		assertThat(calendar).startsWith("- 이번 주: 2026-11-16(월)");
	}
}
