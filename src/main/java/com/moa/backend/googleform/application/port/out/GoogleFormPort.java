package com.moa.backend.googleform.application.port.out;

import com.moa.backend.googleform.domain.CreatedForm;
import com.moa.backend.googleform.domain.FormDraft;

/** 폼을 실제로 만드는 곳(Google Forms API)에 대한 출력 포트. */
public interface GoogleFormPort {

    CreatedForm create(FormDraft draft);
}
