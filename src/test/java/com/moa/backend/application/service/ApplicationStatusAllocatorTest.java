package com.moa.backend.application.service;

import static com.moa.backend.application.entity.ApplicationStatus.CANCELED;
import static com.moa.backend.application.entity.ApplicationStatus.CONFIRMED;
import static com.moa.backend.application.entity.ApplicationStatus.WAITING;
import static org.assertj.core.api.Assertions.assertThat;

import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationSource;
import com.moa.backend.application.entity.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationStatusAllocatorTest {

	private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 14, 9, 0);

	@Test
	void 정원까지_확정_넘으면_대기() {
		List<Application> apps = List.of(app("1", WAITING), app("2", WAITING), app("3", CONFIRMED));

		ApplicationStatusAllocator.Result result = ApplicationStatusAllocator.allocate(apps, 2);

		assertThat(apps).extracting(Application::getStatus).containsExactly(CONFIRMED, CONFIRMED, WAITING);
		assertThat(result).isEqualTo(new ApplicationStatusAllocator.Result(2, 1));
	}

	@Test
	void 정원과_인원이_같으면_모두_확정() {
		List<Application> apps = List.of(app("1", WAITING), app("2", WAITING));

		ApplicationStatusAllocator.Result result = ApplicationStatusAllocator.allocate(apps, 2);

		assertThat(apps).extracting(Application::getStatus).containsOnly(CONFIRMED);
		assertThat(result).isEqualTo(new ApplicationStatusAllocator.Result(2, 0));
	}

	@Test
	void 정원이_0이면_모두_대기() {
		List<Application> apps = List.of(app("1", CONFIRMED));

		assertThat(ApplicationStatusAllocator.allocate(apps, 0)).isEqualTo(new ApplicationStatusAllocator.Result(0, 1));
		assertThat(apps.get(0).getStatus()).isEqualTo(WAITING);
	}

	@Test
	void 앞에_같은_학번이_있으면_자리를_차지하지_않고_대기() {
		List<Application> apps = List.of(app("2024 1234", WAITING), app("20241234", WAITING), app("5", WAITING));

		ApplicationStatusAllocator.Result result = ApplicationStatusAllocator.allocate(apps, 2);

		assertThat(apps).extracting(Application::getStatus).containsExactly(CONFIRMED, WAITING, CONFIRMED);
		assertThat(result).isEqualTo(new ApplicationStatusAllocator.Result(2, 1));
	}

	@Test
	void 학번이_없거나_공백이면_중복으로_보지_않는다() {
		List<Application> apps = List.of(app(null, WAITING), app(null, WAITING), app("  ", WAITING), app(" ", WAITING));

		ApplicationStatusAllocator.allocate(apps, 10);

		assertThat(apps).extracting(Application::getStatus).containsOnly(CONFIRMED);
	}

	@Test
	void 취소는_건드리지_않고_자리와_중복_판별에서도_빠진다() {
		List<Application> apps = List.of(app("1", CANCELED), app("1", WAITING), app("2", CONFIRMED), app("3", WAITING));

		ApplicationStatusAllocator.Result result = ApplicationStatusAllocator.allocate(apps, 2);

		assertThat(apps).extracting(Application::getStatus).containsExactly(CANCELED, CONFIRMED, CONFIRMED, WAITING);
		assertThat(result).isEqualTo(new ApplicationStatusAllocator.Result(2, 1));
	}

	private int sequence = 0;

	private Application app(String studentId, ApplicationStatus status) {
		return Application.builder()
				.name("신청자")
				.studentId(studentId)
				.status(status)
				.source(ApplicationSource.CSV)
				.appliedAt(BASE.plusMinutes(sequence++))
				.build();
	}
}
