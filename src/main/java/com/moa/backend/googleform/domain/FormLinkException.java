package com.moa.backend.googleform.domain;

/** 구글폼 링크를 확인하지 못했을 때. 메시지는 운영진에게 그대로 보여줄 안내 문구다. */
public class FormLinkException extends RuntimeException {

    public FormLinkException(String message) {
        super(message);
    }

    public FormLinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
