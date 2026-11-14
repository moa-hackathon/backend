package com.moa.backend.notice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FormLinkPlaceholderTest {

	@Test
	void 자리_표시가_있으면_그대로_둔다() {
		String text = "공지\n신청 링크: [구글폼 링크]";

		assertThat(FormLinkPlaceholder.ensure(text)).isEqualTo(text);
	}

	@Test
	void 자리_표시가_없으면_마지막_줄에_붙인다() {
		assertThat(FormLinkPlaceholder.ensure("공지 본문\n")).isEqualTo("공지 본문\n신청 링크: [구글폼 링크]");
	}

	@Test
	void 지어낸_링크를_자리_표시로_바꾼다() {
		assertThat(FormLinkPlaceholder.ensure("신청: https://forms.gle/abc123 로 신청하세요"))
				.isEqualTo("신청: [구글폼 링크] 로 신청하세요");
	}

	@Test
	void 살짝_다르게_쓴_자리_표시를_맞춘다() {
		assertThat(FormLinkPlaceholder.ensure("신청 링크: [구글 폼 링크]")).isEqualTo("신청 링크: [구글폼 링크]");
		assertThat(FormLinkPlaceholder.ensure("신청 링크: {구글폼링크}")).isEqualTo("신청 링크: [구글폼 링크]");
	}
}
