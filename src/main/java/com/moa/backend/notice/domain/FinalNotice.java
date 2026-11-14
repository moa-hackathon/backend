package com.moa.backend.notice.domain;

import org.springframework.util.StringUtils;

/**
 * 발송할 최종 공지. 저장된 공지 본문의 {@code [구글폼 링크]} 자리를 구글폼 링크로 바꾼다.
 *
 * @param text           최종 공지 본문. 링크가 아직 없으면 자리 표시가 그대로 남는다.
 * @param formLinkMissing 구글폼 링크가 아직 연결되지 않았는지
 */
public record FinalNotice(String text, boolean formLinkMissing) {

	public static final String FORM_LINK_PLACEHOLDER = "[구글폼 링크]";

	/**
	 * 본문에 자리 표시가 없으면 링크가 빠지지 않도록 마지막 줄에 신청 링크를 붙인다.
	 * 본문에 이미 같은 링크가 있으면 붙이지 않는다.
	 */
	public static FinalNotice of(String noticeText, String formUrl) {
		if (!StringUtils.hasText(formUrl)) {
			return new FinalNotice(noticeText, true);
		}
		String url = formUrl.strip();
		if (noticeText.contains(FORM_LINK_PLACEHOLDER)) {
			return new FinalNotice(noticeText.replace(FORM_LINK_PLACEHOLDER, url), false);
		}
		if (noticeText.contains(url)) {
			return new FinalNotice(noticeText, false);
		}
		return new FinalNotice(noticeText.stripTrailing() + "\n신청 링크: " + url, false);
	}
}
