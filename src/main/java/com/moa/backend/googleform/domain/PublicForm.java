package com.moa.backend.googleform.domain;

import java.util.List;

/**
 * 공개된 구글폼 페이지에서 읽은 내용.
 *
 * @param title 폼 제목 (없으면 null)
 * @param items 응답을 받는 문항만 순서대로. 섹션 제목, 설명 글, 이미지 등은 빠진다.
 */
public record PublicForm(String title, List<PublicFormItem> items) {

    public PublicForm {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * 응답자가 답하는 문항 하나.
     *
     * @param type 구글폼 내부 문항 유형 번호를 옮긴 값. 모르는 번호면 {@code OTHER}.
     */
    public record PublicFormItem(String title, boolean required, PublicFormItemType type) {
    }

    /** 공개 폼 페이지에 실린 문항 유형. 번호는 구글폼 페이지 데이터의 값이다. */
    public enum PublicFormItemType {
        SHORT_ANSWER(0),
        PARAGRAPH(1),
        MULTIPLE_CHOICE(2),
        DROPDOWN(3),
        CHECKBOX(4),
        LINEAR_SCALE(5),
        GRID(7),
        DATE(9),
        TIME(10),
        FILE_UPLOAD(13),
        RATING(18),
        OTHER(-1);

        private final int code;

        PublicFormItemType(int code) {
            this.code = code;
        }

        public static PublicFormItemType fromCode(int code) {
            for (PublicFormItemType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return OTHER;
        }
    }
}
