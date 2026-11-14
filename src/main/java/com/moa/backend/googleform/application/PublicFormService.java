package com.moa.backend.googleform.application;

import com.moa.backend.googleform.application.port.in.ReadPublicFormUseCase;
import com.moa.backend.googleform.application.port.out.PublicFormPagePort;
import com.moa.backend.googleform.domain.GoogleFormLink;
import com.moa.backend.googleform.domain.PublicForm;
import org.springframework.stereotype.Service;

@Service
public class PublicFormService implements ReadPublicFormUseCase {

    private final PublicFormPagePort publicFormPagePort;

    public PublicFormService(PublicFormPagePort publicFormPagePort) {
        this.publicFormPagePort = publicFormPagePort;
    }

    @Override
    public PublicForm read(String formUrl) {
        return publicFormPagePort.read(GoogleFormLink.parse(formUrl));
    }
}
