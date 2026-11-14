package com.moa.backend.dashboard.domain;

import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationSource;
import com.moa.backend.application.entity.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;

public final class ApplicationFixture {

	public static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 14, 9, 0);

	private ApplicationFixture() {
	}

	public static Application application(long id, String name, String studentId, String contact,
			ApplicationStatus status, int minutesAfterBase, Map<String, String> answers) {
		Application application = Application.builder()
				.name(name)
				.studentId(studentId)
				.contact(contact)
				.status(status)
				.source(ApplicationSource.CSV)
				.answers(answers == null ? null : new LinkedHashMap<>(answers))
				.appliedAt(BASE.plusMinutes(minutesAfterBase))
				.build();
		ReflectionTestUtils.setField(application, "id", id);
		return application;
	}

	public static Application application(long id, String name, String studentId, String contact,
			ApplicationStatus status, int minutesAfterBase) {
		return application(id, name, studentId, contact, status, minutesAfterBase, null);
	}
}
