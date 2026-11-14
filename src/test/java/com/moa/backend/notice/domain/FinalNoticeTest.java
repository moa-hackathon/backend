package com.moa.backend.notice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FinalNoticeTest {

	@Test
	void 자리_표시를_구글폼_링크로_바꾼다() {
		FinalNotice notice = FinalNotice.of("[모아] 안내\n신청 링크: [구글폼 링크]", " https://forms.gle/abc ");

		assertThat(notice.text()).isEqualTo("[모아] 안내\n신청 링크: https://forms.gle/abc");
		assertThat(notice.formLinkMissing()).isFalse();
	}

	@Test
	void 링크가_없으면_자리_표시를_그대로_둔다() {
		FinalNotice notice = FinalNotice.of("신청 링크: [구글폼 링크]", null);

		assertThat(notice.text()).isEqualTo("신청 링크: [구글폼 링크]");
		assertThat(notice.formLinkMissing()).isTrue();
	}

	@Test
	void 자리_표시가_없으면_마지막에_링크를_붙인다() {
		FinalNotice notice = FinalNotice.of("[모아] 안내\n", "https://forms.gle/abc");

		assertThat(notice.text()).isEqualTo("[모아] 안내\n신청 링크: https://forms.gle/abc");
	}

	@Test
	void 본문에_이미_링크가_있으면_붙이지_않는다() {
		FinalNotice notice = FinalNotice.of("신청: https://forms.gle/abc", "https://forms.gle/abc");

		assertThat(notice.text()).isEqualTo("신청: https://forms.gle/abc");
	}
}
