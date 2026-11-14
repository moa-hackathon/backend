package com.moa.backend.notice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.moa.backend.event.domain.EventDetails;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoticeFactsTest {

	private static final EventDetails DETAILS = EventDetails.of("모아 네트워킹 행사",
			LocalDateTime.of(2026, 11, 21, 19, 0), "공학관 302호", 40, 20, LocalDateTime.of(2026, 11, 19, 23, 59),
			List.of("뒤풀이 참석 여부"), "피자 나와요");

	@Test
	void 디자인의_공지_초안과_같은_양식을_만든다() {
		assertThat(NoticeFacts.of("모아", DETAILS).renderTemplate()).isEqualTo("""
				[모아] 네트워킹 행사 안내
				일시: 11/21(토) 19:00
				장소: 공학관 302호
				정원: 40명 (선착순)
				신청 마감: 11/19(목)
				피자 나와요
				신청 링크: [구글폼 링크]""");
	}

	@Test
	void 행사명이_동아리명으로_시작하지_않으면_그대로_쓴다() {
		assertThat(NoticeFacts.of("오르카", DETAILS).heading()).isEqualTo("[오르카] 모아 네트워킹 행사 안내");
	}

	@Test
	void 신청_마감이_자정_전후가_아니면_시각까지_쓴다() {
		EventDetails details = EventDetails.of("행사", LocalDateTime.of(2026, 11, 21, 19, 0), "장소", 40, 0,
				LocalDateTime.of(2026, 11, 19, 18, 0), null, null);

		assertThat(NoticeFacts.of("모아", details).applyDeadline()).isEqualTo("11/19(목) 18:00");
	}

	@Test
	void 사실이_모두_있는지_띄어쓰기를_무시하고_확인한다() {
		NoticeFacts facts = NoticeFacts.of("모아", DETAILS);

		assertThat(facts.coveredBy("일시: 11/21(토)19:00\n장소: 공학관302호\n정원 40명\n마감 11/19(목)")).isTrue();
		assertThat(facts.coveredBy("일시: 11/22(일) 19:00\n장소: 공학관 302호\n정원 40명\n마감 11/19(목)")).isFalse();
		assertThat(facts.coveredBy("일시: 11/21(토) 19:00\n장소: 학생회관\n정원 40명\n마감 11/19(목)")).isFalse();
	}
}
