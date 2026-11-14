package com.moa.backend.notice.dto;

/** 공지 초안을 만든 방식. */
public enum NoticeDraftSource {
	/** AI가 작성했다. */
	AI,
	/** AI 응답을 쓸 수 없어 기본 양식으로 만들었다. */
	TEMPLATE
}
