package com.moa.backend.googleform.domain;

/**
 * @param responderUrl 응답자에게 공유할 링크
 * @param editUrl      폼 편집 링크 (폼 소유자만 열 수 있음)
 */
public record CreatedForm(String formId, String responderUrl, String editUrl) {
}
