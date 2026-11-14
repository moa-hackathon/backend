package com.moa.backend.googleform.domain;

/** Google 폼 질문 유형. */
public enum QuestionType {
    /** 단답형 */
    SHORT_ANSWER,
    /** 장문형 */
    PARAGRAPH,
    /** 객관식(하나 선택) */
    MULTIPLE_CHOICE,
    /** 체크박스(여러 개 선택) */
    CHECKBOX,
    /** 드롭다운 */
    DROPDOWN,
    /** 선형 배율 (예: 1~5) */
    LINEAR_SCALE,
    /** 날짜 */
    DATE,
    /** 시간 */
    TIME;

    public boolean hasOptions() {
        return this == MULTIPLE_CHOICE || this == CHECKBOX || this == DROPDOWN;
    }
}
