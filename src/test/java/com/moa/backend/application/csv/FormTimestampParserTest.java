package com.moa.backend.application.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class FormTimestampParserTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"2026/11/14 9:12:00 오전 GMT+9 | 2026-11-14T09:12:00",
			"2026/11/14 9:12:00 오후 GMT+9 | 2026-11-14T21:12:00",
			"2026/11/14 9:12:00 AM GMT+9 | 2026-11-14T09:12:00",
			"2026/11/14 9:12:00 PM GMT+09:00 | 2026-11-14T21:12:00",
			"2026. 11. 14 오전 9:12:00 | 2026-11-14T09:12:00",
			"2026. 11. 14. 오후 1:05:09 | 2026-11-14T13:05:09",
			"11/14/2026 9:12:00 | 2026-11-14T09:12:00",
			"11/14/2026 21:12:00 | 2026-11-14T21:12:00",
			"11/14/2026 9:12:00 PM | 2026-11-14T21:12:00",
			"11/14/2026 9:12:00PM | 2026-11-14T21:12:00",
			"2026-11-14 09:12:00 | 2026-11-14T09:12:00",
			"2026-11-14T09:12:00+09:00 | 2026-11-14T09:12:00",
			"2026-11-14T09:12:00Z | 2026-11-14T09:12:00",
			"2026-11-14 09:12:00.123 | 2026-11-14T09:12:00",
			"2026-11-14 9:12 | 2026-11-14T09:12:00",
			"2026년 11월 14일 오후 9시 12분 | 2026-11-14T21:12:00",
			"2026/11/14 12:30:00 오전 | 2026-11-14T00:30:00",
			"2026/11/14 12:30:00 오후 | 2026-11-14T12:30:00",
			"2026/11/14 9:12:00 UTC | 2026-11-14T09:12:00",
			"2026/11/14 9:12:00 KST | 2026-11-14T09:12:00",
	})
	void 여러_로케일_형식을_읽는다(String raw, LocalDateTime expected) {
		assertThat(FormTimestampParser.parse(raw)).contains(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			"   ",
			"어제 오후",
			"2026/11/14",
			"2026/13/14 9:12:00",
			"2026/02/30 9:12:00",
			"2026/11/14 25:00:00",
			"2026/11/14 13:00:00 오후",
			"2026/11/14 0:10:00 AM",
			"2026/11/14 9:12:00 오전 오후",
			"26/11/14 9:12:00",
			"Nov 14, 2026 9:12:00 AM",
			"46340.3833",
	})
	void 해석할_수_없으면_비어_있다(String raw) {
		assertThat(FormTimestampParser.parse(raw)).isEmpty();
	}

	@Test
	void null은_비어_있다() {
		assertThat(FormTimestampParser.parse(null)).isEmpty();
	}

	@Test
	void 일이_먼저인_형식은_dayFirst로_읽는다() {
		assertThat(FormTimestampParser.parse("05/11/2026 21:12:00", true))
				.contains(LocalDateTime.of(2026, 11, 5, 21, 12));
		assertThat(FormTimestampParser.parse("05/11/2026 21:12:00", false))
				.contains(LocalDateTime.of(2026, 5, 11, 21, 12));
	}

	@Test
	void 첫_숫자가_12를_넘는_값이_있으면_일이_먼저다() {
		assertThat(FormTimestampParser.detectDayFirst(List.of("05/11/2026 9:00:00", "14/11/2026 9:00:00"))).isTrue();
	}

	@Test
	void 그렇지_않으면_월이_먼저다() {
		assertThat(FormTimestampParser.detectDayFirst(List.of("05/11/2026 9:00:00", "11/14/2026 9:00:00"))).isFalse();
		assertThat(FormTimestampParser.detectDayFirst(List.of("2026/11/14 9:12:00 오전 GMT+9"))).isFalse();
		assertThat(FormTimestampParser.detectDayFirst(List.of())).isFalse();
	}
}
