package com.moa.backend.dashboard.domain;

import static com.moa.backend.application.entity.ApplicationStatus.CANCELED;
import static com.moa.backend.application.entity.ApplicationStatus.CONFIRMED;
import static com.moa.backend.application.entity.ApplicationStatus.WAITING;
import static com.moa.backend.dashboard.domain.ApplicationFixture.application;
import static org.assertj.core.api.Assertions.assertThat;

import com.moa.backend.application.entity.Application;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnswerAggregatorTest {

	private static final String AFTER_PARTY = "뒤풀이 참석 여부";

	@Test
	void 문항별로_값마다_인원과_신청_인원_대비_비율을_센다() {
		List<Application> applications = List.of(
				application(1, "a", "1", "c", CONFIRMED, 0, Map.of(AFTER_PARTY, "참석")),
				application(2, "b", "2", "c", CONFIRMED, 1, Map.of(AFTER_PARTY, "불참")),
				application(3, "c", "3", "c", WAITING, 2, Map.of(AFTER_PARTY, " 참석 ")),
				application(4, "d", "4", "c", CONFIRMED, 3, Map.of(AFTER_PARTY, "")),
				application(5, "e", "5", "c", CANCELED, 4, Map.of(AFTER_PARTY, "불참")));

		List<AnswerStat> stats = AnswerAggregator.aggregate(ApplicationReview.of(applications));

		assertThat(stats).hasSize(1);
		AnswerStat stat = stats.get(0);
		assertThat(stat.question()).isEqualTo(AFTER_PARTY);
		assertThat(stat.answeredCount()).isEqualTo(3);
		assertThat(stat.options()).containsExactly(
				new AnswerStat.Option("참석", 2, 50),
				new AnswerStat.Option("불참", 1, 25));
	}

	@Test
	void 문항은_저장된_폼_문항_순서대로_집계한다() {
		Map<String, String> answers = new LinkedHashMap<>();
		answers.put("첫 참여", "예");
		answers.put(AFTER_PARTY, "참석");
		List<Application> applications = List.of(
				application(1, "a", "1", "c", CONFIRMED, 0, answers),
				application(2, "b", "2", "c", CONFIRMED, 1, answers));

		List<AnswerStat> stats = AnswerAggregator.aggregate(
				ApplicationReview.of(applications, List.of(AFTER_PARTY, "첫 참여")));

		assertThat(stats).extracting(AnswerStat::question).containsExactly(AFTER_PARTY, "첫 참여");
	}

	@Test
	void 인원이_같으면_먼저_나온_값이_앞선다() {
		List<Application> applications = List.of(
				application(1, "a", "1", "c", CONFIRMED, 0, Map.of("학년", "3학년")),
				application(2, "b", "2", "c", CONFIRMED, 1, Map.of("학년", "1학년")),
				application(3, "c", "3", "c", CONFIRMED, 2, Map.of("학년", "1학년")),
				application(4, "d", "4", "c", CONFIRMED, 3, Map.of("학년", "3학년")));

		assertThat(AnswerAggregator.aggregate(ApplicationReview.of(applications)).get(0).options())
				.extracting(AnswerStat.Option::value).containsExactly("3학년", "1학년");
	}

	@Test
	void 값_종류가_상한을_넘는_문항은_서술형으로_보고_뺀다() {
		List<Application> applications = new ArrayList<>();
		for (int i = 0; i <= AnswerAggregator.MAX_DISTINCT_VALUES; i++) {
			Map<String, String> answers = new LinkedHashMap<>();
			answers.put(AFTER_PARTY, i % 2 == 0 ? "참석" : "불참");
			answers.put("하고 싶은 말", "한마디 " + i);
			applications.add(application(i + 1, "n", String.valueOf(i), "c", CONFIRMED, i, answers));
		}

		List<AnswerStat> stats = AnswerAggregator.aggregate(ApplicationReview.of(applications));

		assertThat(stats).extracting(AnswerStat::question).containsExactly(AFTER_PARTY);
	}

	@Test
	void 값_종류가_상한과_같으면_집계한다() {
		List<Application> applications = new ArrayList<>();
		for (int i = 0; i < AnswerAggregator.MAX_DISTINCT_VALUES * 2; i++) {
			applications.add(application(i + 1, "n", String.valueOf(i), "c", CONFIRMED, i,
					Map.of("학과", "학과" + (i % AnswerAggregator.MAX_DISTINCT_VALUES))));
		}

		assertThat(AnswerAggregator.aggregate(ApplicationReview.of(applications))).hasSize(1);
	}

	@Test
	void 두_명_이상이_고른_값이_없는_문항은_서술형으로_보고_뺀다() {
		Map<String, String> first = new LinkedHashMap<>();
		first.put(AFTER_PARTY, "참석");
		first.put("하고 싶은 말", "기대돼요");
		Map<String, String> second = new LinkedHashMap<>();
		second.put(AFTER_PARTY, "참석");
		second.put("하고 싶은 말", "잘 부탁드려요");
		List<Application> applications = List.of(
				application(1, "a", "1", "c", CONFIRMED, 0, first),
				application(2, "b", "2", "c", CONFIRMED, 1, second),
				application(3, "c", "3", "c", CONFIRMED, 2, Map.of(AFTER_PARTY, "불참")));

		List<AnswerStat> stats = AnswerAggregator.aggregate(ApplicationReview.of(applications));

		assertThat(stats).extracting(AnswerStat::question).containsExactly(AFTER_PARTY);
	}

	@Test
	void 긴_값이_있는_문항은_서술형으로_보고_뺀다() {
		String longText = "가".repeat(AnswerAggregator.MAX_VALUE_LENGTH + 1);
		List<Application> applications = List.of(
				application(1, "a", "1", "c", CONFIRMED, 0, Map.of("소감", longText)),
				application(2, "b", "2", "c", CONFIRMED, 1, Map.of("소감", "좋아요")));

		assertThat(AnswerAggregator.aggregate(ApplicationReview.of(applications))).isEmpty();
	}

	@Test
	void 아무도_답하지_않은_문항과_신청이_없는_경우는_비어_있다() {
		assertThat(AnswerAggregator.aggregate(ApplicationReview.of(List.of()))).isEmpty();
		assertThat(AnswerAggregator.aggregate(ApplicationReview.of(List.of(
				application(1, "a", "1", "c", CONFIRMED, 0, Map.of("메모", " ")))))).isEmpty();
	}
}
