package com.moa.backend.dashboard.domain;

import static com.moa.backend.application.entity.ApplicationStatus.CANCELED;
import static com.moa.backend.application.entity.ApplicationStatus.CONFIRMED;
import static com.moa.backend.application.entity.ApplicationStatus.WAITING;
import static com.moa.backend.dashboard.domain.ApplicationFixture.application;
import static org.assertj.core.api.Assertions.assertThat;

import com.moa.backend.application.entity.Application;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationReviewTest {

	@Test
	void 같은_학번의_가장_이른_신청을_빼고_나머지를_중복으로_본다() {
		ApplicationReview review = ApplicationReview.of(List.of(
				application(3, "김모아", "2023 0015", "010-1", WAITING, 30),
				application(1, "김모아", "20230015", "010-1", CONFIRMED, 10),
				application(2, "이모아", "20240001", "010-2", CONFIRMED, 20),
				application(4, "김모아", " 20230015 ", "010-1", WAITING, 40)));

		assertThat(review.rows()).extracting(row -> row.application().getId()).containsExactly(1L, 2L, 3L, 4L);
		assertThat(review.rows()).extracting(ReviewedApplication::duplicate).containsExactly(false, false, true, true);
		assertThat(review.rows()).extracting(ReviewedApplication::displayStatus).containsExactly(
				DisplayStatus.CONFIRMED, DisplayStatus.CONFIRMED, DisplayStatus.DUPLICATE, DisplayStatus.DUPLICATE);
		assertThat(review.duplicateCount()).isEqualTo(2);
	}

	@Test
	void 신청_시각이_같으면_id가_작은_행을_원본으로_본다() {
		ApplicationReview review = ApplicationReview.of(List.of(
				application(8, "김모아", "20230015", "010-1", WAITING, 0),
				application(5, "김모아", "20230015", "010-1", CONFIRMED, 0)));

		assertThat(review.rows()).extracting(row -> row.application().getId()).containsExactly(5L, 8L);
		assertThat(review.rows()).extracting(ReviewedApplication::duplicate).containsExactly(false, true);
	}

	@Test
	void 취소된_행은_중복_판정에서_빠지고_취소로만_표시된다() {
		ApplicationReview review = ApplicationReview.of(List.of(
				application(1, "김모아", "20230015", "010-1", CANCELED, 0),
				application(2, "김모아", "20230015", "010-1", CONFIRMED, 10),
				application(3, "", "", null, CANCELED, 20)));

		assertThat(review.rows()).extracting(ReviewedApplication::displayStatus)
				.containsExactly(DisplayStatus.CANCELED, DisplayStatus.CONFIRMED, DisplayStatus.CANCELED);
		assertThat(review.duplicateCount()).isZero();
		assertThat(review.missingCount()).isZero();
		assertThat(review.applicantCount()).isEqualTo(1);
	}

	@Test
	void 빈_학번은_중복으로_묶지_않고_누락으로_본다() {
		ApplicationReview review = ApplicationReview.of(List.of(
				application(1, "김모아", null, "010-1", CONFIRMED, 0),
				application(2, "이모아", "  ", "010-2", CONFIRMED, 10)));

		assertThat(review.duplicateCount()).isZero();
		assertThat(review.rows()).extracting(ReviewedApplication::displayStatus)
				.containsExactly(DisplayStatus.MISSING, DisplayStatus.MISSING);
		assertThat(review.rows().get(0).missingFields()).containsExactly(RequiredField.STUDENT_ID);
	}

	@Test
	void 이름_학번_연락처_중_빈_항목을_모두_기록한다() {
		ApplicationReview review = ApplicationReview.of(List.of(
				application(1, " ", "20230001", "", WAITING, 0),
				application(2, "이모아", "20230002", null, CONFIRMED, 10),
				application(3, "박모아", "20230003", "010-3", CONFIRMED, 20)));

		assertThat(review.rows().get(0).missingFields()).containsExactly(RequiredField.NAME, RequiredField.CONTACT);
		assertThat(review.rows().get(1).missingFields()).containsExactly(RequiredField.CONTACT);
		assertThat(review.rows().get(2).missing()).isFalse();
		assertThat(review.missingCount()).isEqualTo(2);
		assertThat(review.missingFieldCounts())
				.containsExactly(Map.entry(RequiredField.NAME, 1), Map.entry(RequiredField.CONTACT, 2));
	}

	@Test
	void 중복이면서_누락이면_중복으로_표시하고_건수는_각각_센다() {
		ApplicationReview review = ApplicationReview.of(List.of(
				application(1, "김모아", "20230015", "010-1", CONFIRMED, 0),
				application(2, "김모아", "20230015", null, WAITING, 10)));

		ReviewedApplication second = review.rows().get(1);
		assertThat(second.displayStatus()).isEqualTo(DisplayStatus.DUPLICATE);
		assertThat(second.duplicate()).isTrue();
		assertThat(second.missing()).isTrue();
		assertThat(review.duplicateCount()).isEqualTo(1);
		assertThat(review.missingCount()).isEqualTo(1);
		assertThat(review.needsCheckCount()).isEqualTo(1);
	}

	@Test
	void 저장된_상태에_따라_확정과_대기로_표시한다() {
		ApplicationReview review = ApplicationReview.of(List.of(
				application(1, "김모아", "1", "010-1", CONFIRMED, 0),
				application(2, "이모아", "2", "010-2", WAITING, 10)));

		assertThat(review.rows()).extracting(ReviewedApplication::displayStatus)
				.containsExactly(DisplayStatus.CONFIRMED, DisplayStatus.WAITING);
		assertThat(review.rows()).extracting(row -> row.displayStatus().getLabel()).containsExactly("확정", "대기");
	}

	@Test
	void 필터는_저장된_상태와_확인_필요_여부로_거른다() {
		List<Application> applications = List.of(
				application(1, "김모아", "1", "010-1", CONFIRMED, 0),
				application(2, "이모아", "2", null, CONFIRMED, 10),
				application(3, "박모아", "3", "010-3", WAITING, 20),
				application(4, "김모아", "1", "010-1", WAITING, 30),
				application(5, "최모아", "5", "010-5", CANCELED, 40));
		ApplicationReview review = ApplicationReview.of(applications);

		assertThat(ids(review.filter(ApplicationFilter.ALL))).containsExactly(1L, 2L, 3L, 4L, 5L);
		assertThat(ids(review.filter(ApplicationFilter.CONFIRMED))).containsExactly(1L, 2L);
		assertThat(ids(review.filter(ApplicationFilter.WAITING))).containsExactly(3L, 4L);
		assertThat(ids(review.filter(ApplicationFilter.NEEDS_CHECK))).containsExactly(2L, 4L);
	}

	@Test
	void 문항은_신청_순으로_처음_나온_순서를_유지한_합집합이다() {
		ApplicationReview review = ApplicationReview.of(List.of(
				application(2, "이모아", "2", "010-2", CONFIRMED, 10, Map.of("첫 참여", "예")),
				application(1, "김모아", "1", "010-1", CONFIRMED, 0, Map.of("뒤풀이 참석 여부", "참석")),
				application(3, "박모아", "3", "010-3", CONFIRMED, 20, null)));

		assertThat(review.questions()).containsExactly("뒤풀이 참석 여부", "첫 참여");
	}

	private static List<Long> ids(List<ReviewedApplication> rows) {
		return rows.stream().map(row -> row.application().getId()).toList();
	}
}
