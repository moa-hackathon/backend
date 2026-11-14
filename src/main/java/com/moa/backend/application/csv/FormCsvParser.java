package com.moa.backend.application.csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/** 디코딩된 구글폼 응답 CSV 문자열을 적재할 행과 실패한 행으로 나눈다. DB와 무관한 순수 로직이다. */
public final class FormCsvParser {

	/** application 테이블 컬럼 길이와 맞춘다. */
	static final int NAME_MAX_LENGTH = 50;
	static final int STUDENT_ID_MAX_LENGTH = 20;
	static final int CONTACT_MAX_LENGTH = 100;

	private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
			.setIgnoreEmptyLines(true)
			.get();

	private FormCsvParser() {
	}

	public static ParsedFormCsv parse(String text) {
		List<CSVRecord> records = readRecords(text);
		if (records.isEmpty()) {
			throw new CsvReadException(CsvReadException.Reason.EMPTY, "빈 파일입니다.");
		}
		FormColumns columns = FormColumns.resolve(records.get(0).toList());
		List<CSVRecord> dataRecords = records.subList(1, records.size()).stream()
				.filter(record -> !isBlank(record))
				.toList();

		List<String> timestamps = new ArrayList<>(dataRecords.size());
		for (CSVRecord record : dataRecords) {
			timestamps.add(valueAt(record, columns.timestamp()));
		}
		boolean dayFirst = FormTimestampParser.detectDayFirst(timestamps);

		List<FormResponseRow> rows = new ArrayList<>();
		List<CsvRowError> errors = new ArrayList<>();
		for (CSVRecord record : dataRecords) {
			long rowNumber = record.getRecordNumber();
			try {
				rows.add(toRow(record, columns, dayFirst));
			} catch (RowRejected e) {
				errors.add(new CsvRowError(rowNumber, e.getMessage()));
			}
		}
		return new ParsedFormCsv(dataRecords.size(), Collections.unmodifiableList(rows),
				Collections.unmodifiableList(errors));
	}

	private static FormResponseRow toRow(CSVRecord record, FormColumns columns, boolean dayFirst) {
		for (int i = columns.width(); i < record.size(); i++) {
			if (!record.get(i).isBlank()) {
				throw new RowRejected("헤더보다 값이 많은 행입니다.");
			}
		}
		List<String> fields = new ArrayList<>(columns.width());
		for (int i = 0; i < columns.width(); i++) {
			fields.add(normalizeLineBreaks(valueAt(record, i)));
		}

		String rawTimestamp = fields.get(columns.timestamp()).strip();
		if (rawTimestamp.isEmpty()) {
			throw new RowRejected("타임스탬프가 비어 있습니다.");
		}
		Optional<LocalDateTime> appliedAt = FormTimestampParser.parse(rawTimestamp, dayFirst);
		if (appliedAt.isEmpty()) {
			throw new RowRejected("타임스탬프 형식을 해석할 수 없습니다: " + rawTimestamp);
		}

		String name = fields.get(columns.name()).strip();
		if (name.isEmpty()) {
			throw new RowRejected("이름이 비어 있습니다.");
		}
		checkLength(name, NAME_MAX_LENGTH, "이름이");
		String studentId = optionalValue(fields, columns.studentId());
		checkLength(studentId, STUDENT_ID_MAX_LENGTH, "학번이");
		String contact = optionalValue(fields, columns.contact());
		checkLength(contact, CONTACT_MAX_LENGTH, "연락처가");

		Map<String, String> answers = new LinkedHashMap<>();
		columns.answerKeys().forEach((index, key) -> answers.put(key, fields.get(index).strip()));

		return new FormResponseRow(record.getRecordNumber(), name, studentId, contact, appliedAt.get(),
				Collections.unmodifiableMap(answers), RowHasher.hash(fields));
	}

	private static List<CSVRecord> readRecords(String text) {
		try (CSVParser parser = CSVParser.parse(text, FORMAT)) {
			return parser.getRecords();
		} catch (IOException | UncheckedIOException | IllegalStateException e) {
			throw new CsvReadException(CsvReadException.Reason.UNREADABLE,
					"CSV 형식이 올바르지 않습니다. 따옴표가 닫히지 않은 값이 있는지 확인해 주세요.");
		}
	}

	private static String valueAt(CSVRecord record, int index) {
		return index < record.size() ? record.get(index) : "";
	}

	private static boolean isBlank(CSVRecord record) {
		for (String value : record) {
			if (!value.isBlank()) {
				return false;
			}
		}
		return true;
	}

	private static String optionalValue(List<String> fields, int index) {
		if (index == FormColumns.NONE) {
			return null;
		}
		String value = fields.get(index).strip();
		return value.isEmpty() ? null : value;
	}

	private static void checkLength(String value, int max, String subject) {
		if (value != null && value.length() > max) {
			throw new RowRejected(subject + " " + max + "자를 넘습니다.");
		}
	}

	/** 같은 응답이 CRLF/LF 차이로 다른 해시가 되지 않게 줄바꿈을 LF로 맞춘다. */
	private static String normalizeLineBreaks(String value) {
		return value.replace("\r\n", "\n").replace('\r', '\n');
	}

	private static final class RowRejected extends RuntimeException {

		RowRejected(String message) {
			super(message, null, false, false);
		}
	}
}
