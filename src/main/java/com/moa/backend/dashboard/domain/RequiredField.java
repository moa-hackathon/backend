package com.moa.backend.dashboard.domain;

import com.moa.backend.application.entity.Application;
import java.util.function.Function;
import lombok.Getter;

/** 신청서의 필수 항목. 하나라도 비어 있으면 누락으로 본다. */
@Getter
public enum RequiredField {

	NAME("이름", Application::getName),
	STUDENT_ID("학번", Application::getStudentId),
	CONTACT("연락처", Application::getContact);

	private final String label;
	private final Function<Application, String> extractor;

	RequiredField(String label, Function<Application, String> extractor) {
		this.label = label;
		this.extractor = extractor;
	}

	public boolean isBlankIn(Application application) {
		String value = extractor.apply(application);
		return value == null || value.isBlank();
	}
}
