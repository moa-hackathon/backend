package com.moa.backend.googleform.application.port.out;

import com.moa.backend.googleform.domain.GoogleFormLink;
import com.moa.backend.googleform.domain.PublicForm;

/** 로그인 없이 열리는 구글폼 응답 페이지를 읽는 출력 포트. */
public interface PublicFormPagePort {

    /**
     * @throws com.moa.backend.googleform.domain.FormLinkException 열 수 없거나, 폼이 아니거나, 로그인이 필요하거나, 응답을 받지 않는 폼일 때
     */
    PublicForm read(GoogleFormLink link);
}
