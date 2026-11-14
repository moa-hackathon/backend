package com.moa.backend.googleform.application.port.in;

import com.moa.backend.googleform.domain.PublicForm;

public interface ReadPublicFormUseCase {

    /**
     * 운영진이 붙여넣은 구글폼 링크를 검사하고 공개 폼 페이지에서 문항을 읽는다.
     *
     * @throws com.moa.backend.googleform.domain.FormLinkException 링크 형식이 틀렸거나 폼을 읽을 수 없을 때 (메시지는 안내 문구)
     */
    PublicForm read(String formUrl);
}
