package com.moa.backend.event.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EventProgressTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 12, 40);
	private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 11, 19, 23, 59);

	@ParameterizedTest
	@CsvSource({
			"0, 40, 20, BELOW_MIN",
			"9, 30, 15, BELOW_MIN",
			"31, 60, 30, AVAILABLE",
			"31, 40, 20, AVAILABLE",
			"32, 40, 20, NEAR_CAPACITY",
			"34, 40, 20, NEAR_CAPACITY",
			"50, 50, 20, FULL",
			"52, 50, 20, FULL",
	})
	void 신청_인원에_따라_진행_상태를_정한다(int count, int capacity, int min, EventProgressStatus expected) {
		assertThat(EventProgress.of(count, capacity, min, DEADLINE, NOW).status()).isEqualTo(expected);
	}

	@Test
	void 남은_인원과_비율과_디데이를_계산한다() {
		EventProgress progress = EventProgress.of(34, 40, 20, DEADLINE, NOW);

		assertThat(progress.remainingToMin()).isZero();
		assertThat(progress.remainingToCapacity()).isEqualTo(6);
		assertThat(progress.fillPercent()).isEqualTo(85);
		assertThat(progress.daysUntilDeadline()).isEqualTo(5);
		assertThat(progress.deadlinePassed()).isFalse();
	}

	@Test
	void 마감이_지나면_디데이가_음수가_된다() {
		EventProgress progress = EventProgress.of(9, 30, 15, DEADLINE, DEADLINE.plusDays(2));

		assertThat(progress.remainingToMin()).isEqualTo(6);
		assertThat(progress.daysUntilDeadline()).isEqualTo(-2);
		assertThat(progress.deadlinePassed()).isTrue();
	}
}
