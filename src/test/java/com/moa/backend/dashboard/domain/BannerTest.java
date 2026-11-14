package com.moa.backend.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.moa.backend.event.domain.EventProgress;
import java.time.LocalDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BannerTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 12, 40);
	private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 11, 19, 23, 59);

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"0 | 40 | 20 | 필수 인원 미달 | 최소 인원까지 20명 남았어요.",
			"31 | 60 | 30 | 진행 가능 | 최소 인원은 넘었고, 정원까지 29명 남았어요.",
			"34 | 40 | 20 | 진행 가능 · 정원 임박 | 최소 인원은 넘었고, 정원까지 6명 남았어요.",
			"50 | 50 | 20 | 정원 마감 | 정원이 모두 찼어요.",
	})
	void 진행_상태에_맞는_배너_문구를_만든다(int count, int capacity, int min, String title, String message) {
		Banner banner = Banner.from(EventProgress.of(count, capacity, min, DEADLINE, NOW));

		assertThat(banner.title()).isEqualTo(title);
		assertThat(banner.message()).isEqualTo(message);
	}
}
