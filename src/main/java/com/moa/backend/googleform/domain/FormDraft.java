package com.moa.backend.googleform.domain;

import java.util.List;

/** 만들 폼의 내용. */
public record FormDraft(String title, String description, List<FormQuestion> questions) {

    public FormDraft {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("폼 제목이 비어 있습니다.");
        }
        questions = questions == null ? List.of() : List.copyOf(questions);
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }
}
