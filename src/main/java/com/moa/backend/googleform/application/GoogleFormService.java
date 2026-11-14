package com.moa.backend.googleform.application;

import com.moa.backend.googleform.application.port.in.CreateGoogleFormUseCase;
import com.moa.backend.googleform.application.port.out.GoogleFormPort;
import com.moa.backend.googleform.domain.CreatedForm;
import com.moa.backend.googleform.domain.FormDraft;
import org.springframework.stereotype.Service;

@Service
public class GoogleFormService implements CreateGoogleFormUseCase {

    private final GoogleFormPort googleFormPort;

    public GoogleFormService(GoogleFormPort googleFormPort) {
        this.googleFormPort = googleFormPort;
    }

    @Override
    public CreatedForm create(FormDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("폼 내용이 없습니다.");
        }
        return googleFormPort.create(draft);
    }
}
