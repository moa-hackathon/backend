package com.moa.backend.googleform.application.port.in;

import com.moa.backend.googleform.domain.CreatedForm;
import com.moa.backend.googleform.domain.FormDraft;

public interface CreateGoogleFormUseCase {

    /** 연결된 Google 계정으로 폼을 만들고 질문을 넣은 뒤 응답을 받을 수 있게 게시한다. */
    CreatedForm create(FormDraft draft);
}
