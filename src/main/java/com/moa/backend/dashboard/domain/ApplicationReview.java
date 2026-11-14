package com.moa.backend.dashboard.domain;

import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 한 행사의 신청 전체에 대한 중복·누락·표시 상태 판정. 대시보드 요약, 신청자 목록, 엑셀이 모두 이것을 쓴다.
 *
 * <ul>
 *   <li>행 순서: {@code applied_at}, {@code id} 오름차순</li>
 *   <li>중복: 취소되지 않은 행 중 같은 학번(공백 제거, 빈 값 제외)이 2건 이상이면 가장 이른 행을 뺀 나머지</li>
 *   <li>누락: 취소되지 않은 행 중 이름·학번·연락처 중 하나라도 빈 행</li>
 * </ul>
 */
public final class ApplicationReview {

	private static final Comparator<Application> APPLIED_ORDER = Comparator
			.comparing(Application::getAppliedAt, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Application::getId, Comparator.nullsLast(Comparator.naturalOrder()));

	private final List<ReviewedApplication> rows;
	private final List<String> questions;

	private ApplicationReview(List<ReviewedApplication> rows, List<String> questions) {
		this.rows = rows;
		this.questions = questions;
	}

	/** 저장된 폼 문항 순서 없이 판정한다. 문항은 answers에 처음 나온 순서를 따른다. */
	public static ApplicationReview of(List<Application> applications) {
		return of(applications, null);
	}

	/**
	 * @param formQuestions 행사에 저장된 구글폼 문항 순서({@code event.formQuestions}), 없으면 null
	 */
	public static ApplicationReview of(List<Application> applications, List<String> formQuestions) {
		List<Application> sorted = applications.stream().sorted(APPLIED_ORDER).toList();
		Set<String> seenStudentIds = new HashSet<>();
		List<ReviewedApplication> rows = new ArrayList<>(sorted.size());
		for (Application application : sorted) {
			if (application.getStatus() == ApplicationStatus.CANCELED) {
				rows.add(ReviewedApplication.of(application, false, List.of()));
				continue;
			}
			String studentKey = normalizeStudentId(application.getStudentId());
			boolean duplicate = studentKey != null && !seenStudentIds.add(studentKey);
			List<RequiredField> missingFields = new ArrayList<>();
			for (RequiredField field : RequiredField.values()) {
				if (field.isBlankIn(application)) {
					missingFields.add(field);
				}
			}
			rows.add(ReviewedApplication.of(application, duplicate, missingFields));
		}
		List<ReviewedApplication> reviewed = List.copyOf(rows);
		return new ApplicationReview(reviewed, orderQuestions(reviewed, formQuestions));
	}

	/**
	 * 문항 순서: 저장된 폼 문항을 그 순서대로 먼저 두고, 거기에 없지만 answers에 실제로 있는 문항을
	 * 신청 순으로 처음 나온 순서대로 뒤에 붙인다. 엑셀 열, 응답 집계, 목록의 answers가 모두 이 순서를 쓴다.
	 */
	private static List<String> orderQuestions(List<ReviewedApplication> rows, List<String> formQuestions) {
		Set<String> questions = new LinkedHashSet<>();
		if (formQuestions != null) {
			for (String question : formQuestions) {
				if (question != null) {
					questions.add(question);
				}
			}
		}
		for (ReviewedApplication row : rows) {
			Map<String, String> answers = row.application().getAnswers();
			if (answers != null) {
				questions.addAll(answers.keySet());
			}
		}
		return List.copyOf(questions);
	}

	static String normalizeStudentId(String studentId) {
		if (studentId == null) {
			return null;
		}
		String normalized = studentId.replaceAll("\\s+", "");
		return normalized.isEmpty() ? null : normalized;
	}

	/** 취소 행을 포함한 전체 행, 신청 순. */
	public List<ReviewedApplication> rows() {
		return rows;
	}

	public List<ReviewedApplication> filter(ApplicationFilter filter) {
		return rows.stream().filter(filter::matches).toList();
	}

	/** 취소되지 않은 행, 신청 순. */
	public List<ReviewedApplication> activeRows() {
		return rows.stream().filter(row -> !row.canceled()).toList();
	}

	/** 신청 인원: 취소되지 않은 행 수. */
	public int applicantCount() {
		return (int) rows.stream().filter(row -> !row.canceled()).count();
	}

	public int duplicateCount() {
		return (int) rows.stream().filter(ReviewedApplication::duplicate).count();
	}

	public int missingCount() {
		return (int) rows.stream().filter(ReviewedApplication::missing).count();
	}

	public int needsCheckCount() {
		return (int) rows.stream().filter(ReviewedApplication::needsCheck).count();
	}

	/** 필수 항목별로 비어 있는 행 수. 빈 행이 없는 항목은 빠진다. 순서는 이름, 학번, 연락처. */
	public Map<RequiredField, Integer> missingFieldCounts() {
		Map<RequiredField, Integer> counts = new EnumMap<>(RequiredField.class);
		for (ReviewedApplication row : rows) {
			for (RequiredField field : row.missingFields()) {
				counts.merge(field, 1, Integer::sum);
			}
		}
		return counts;
	}

	/** 문항 순서. 저장된 폼 문항 먼저, 그 뒤에 answers에만 있는 문항. */
	public List<String> questions() {
		return questions;
	}

	/** 한 행의 answers를 {@link #questions()} 순서로 다시 담는다. 응답이 없는 문항 키는 넣지 않는다. */
	public Map<String, String> orderedAnswers(Application application) {
		Map<String, String> answers = application.getAnswers();
		Map<String, String> ordered = new LinkedHashMap<>();
		if (answers == null) {
			return ordered;
		}
		for (String question : questions) {
			if (answers.containsKey(question)) {
				ordered.put(question, answers.get(question));
			}
		}
		return ordered;
	}
}
