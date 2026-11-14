package com.moa.backend.dashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.moa.backend.event.domain.EventProgress;
import java.time.LocalDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NextActionTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 12, 40);
	private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 11, 19, 23, 59);

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"0|40|20|RECRUIT_MORE|최소 인원까지 20명 남았어요. 추가 모집 공지를 만들어 드릴까요?",
			"25|40|20|ENCOURAGE|최소 인원은 넘었고 정원까지 15명 남았어요. 신청 독려 공지를 만들어 드릴까요?",
			"34|40|20|CLOSING_SOON|정원까지 6명 남았어요. 마감 임박 공지를 만들어 드릴까요?",
			"40|40|20|ANNOUNCE_CLOSED|정원이 모두 찼어요. 신청 마감 안내 공지를 만들어 드릴까요?",
	})
	void 진행_상태에_맞는_공지를_제안한다(int count, int capacity, int min, NextAction.Type type, String message) {
		NextAction action = NextAction.from(EventProgress.of(count, capacity, min, DEADLINE, NOW));

		assertThat(action.type()).isEqualTo(type);
		assertThat(action.message()).isEqualTo(message);
	}
}
