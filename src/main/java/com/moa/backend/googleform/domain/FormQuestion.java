package com.moa.backend.googleform.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 폼 질문 하나. 유형별 규칙(보기 필수, 배율 범위)을 여기서 검사한다.
 *
 * @param options   MULTIPLE_CHOICE / CHECKBOX / DROPDOWN 에서만 쓴다
 * @param scaleLow  LINEAR_SCALE 최솟값 (0 또는 1, 기본 1)
 * @param scaleHigh LINEAR_SCALE 최댓값 (2~10, 기본 5)
 */
public record FormQuestion(
        QuestionType type,
        String title,
        String description,
        boolean required,
        List<String> options,
        Integer scaleLow,
        Integer scaleHigh,
        String scaleLowLabel,
        String scaleHighLabel) {

    public FormQuestion {
        if (type == null) {
            throw new IllegalArgumentException("질문 유형이 없습니다.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("질문 제목이 비어 있습니다.");
        }
        options = options == null ? List.of() : List.copyOf(options);

        if (type.hasOptions()) {
            validateOptions(title, options);
        } else if (!options.isEmpty()) {
            throw new IllegalArgumentException("'%s': %s 질문에는 보기를 넣을 수 없습니다.".formatted(title, type));
        }

        if (type == QuestionType.LINEAR_SCALE) {
            scaleLow = scaleLow == null ? 1 : scaleLow;
            scaleHigh = scaleHigh == null ? 5 : scaleHigh;
            if (scaleLow != 0 && scaleLow != 1) {
                throw new IllegalArgumentException("'%s': 배율 최솟값은 0 또는 1이어야 합니다.".formatted(title));
            }
            if (scaleHigh < 2 || scaleHigh > 10) {
                throw new IllegalArgumentException("'%s': 배율 최댓값은 2~10 사이여야 합니다.".formatted(title));
            }
        } else {
            scaleLow = null;
            scaleHigh = null;
            scaleLowLabel = null;
            scaleHighLabel = null;
        }
    }

    private static void validateOptions(String title, List<String> options) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("'%s': 보기가 1개 이상 필요합니다.".formatted(title));
        }
        Set<String> seen = new HashSet<>();
        for (String option : options) {
            if (option == null || option.isBlank()) {
                throw new IllegalArgumentException("'%s': 빈 보기가 있습니다.".formatted(title));
            }
            if (!seen.add(option)) {
                throw new IllegalArgumentException("'%s': 보기 '%s'가 중복됩니다.".formatted(title, option));
            }
        }
    }
}
