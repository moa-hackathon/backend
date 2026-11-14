package com.moa.backend.application.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormCsvParserTest {

	private static final String HEADER = "타임스탬프,이름,학번,연락처,뒤풀이 참석 여부,하고 싶은 말\n";

	@Test
	void 따옴표_안의_쉼표와_줄바꿈을_한_값으로_읽는다() {
		String csv = HEADER
				+ "2026/11/14 9:12:00 오전 GMT+9,김모아,20241234,010-1234-5678,참석,\"안녕하세요, 반가워요\n두 줄째 \"\"인용\"\"\"\n"
				+ "2026/11/14 9:30:00 오전 GMT+9,이모아,20231515,010-5678-1234,불참,\n";

		ParsedFormCsv parsed = FormCsvParser.parse(csv);

		assertThat(parsed.totalRows()).isEqualTo(2);
		assertThat(parsed.errors()).isEmpty();
		FormResponseRow first = parsed.rows().get(0);
		assertThat(first.rowNumber()).isEqualTo(2);
		assertThat(first.name()).isEqualTo("김모아");
		assertThat(first.studentId()).isEqualTo("20241234");
		assertThat(first.contact()).isEqualTo("010-1234-5678");
		assertThat(first.appliedAt()).isEqualTo(LocalDateTime.of(2026, 11, 14, 9, 12));
		assertThat(first.answers()).containsExactly(
				Map.entry("뒤풀이 참석 여부", "참석"),
				Map.entry("하고 싶은 말", "안녕하세요, 반가워요\n두 줄째 \"인용\""));
		assertThat(first.rowHash()).hasSize(64);
		// 줄바꿈이 있는 값 다음 레코드 번호도 레코드 기준으로 센다
		assertThat(parsed.rows().get(1).rowNumber()).isEqualTo(3);
	}

	@Test
	void 학번_연락처가_비어_있으면_null로_적재한다() {
		ParsedFormCsv parsed = FormCsvParser.parse(HEADER + "2026/11/14 9:12:00 오전 GMT+9,김모아, ,,참석,\n");

		FormResponseRow row = parsed.rows().get(0);
		assertThat(row.studentId()).isNull();
		assertThat(row.contact()).isNull();
	}

	@Test
	void 학번_연락처_열이_없어도_적재한다() {
		ParsedFormCsv parsed = FormCsvParser.parse("Timestamp,이름을 적어 주세요\n11/14/2026 9:12:00,김모아\n");

		assertThat(parsed.rows()).singleElement()
				.satisfies(row -> {
					assertThat(row.name()).isEqualTo("김모아");
					assertThat(row.studentId()).isNull();
					assertThat(row.answers()).isEmpty();
				});
	}

	@Test
	void 해석하지_못한_행은_행_번호와_사유로_모은다() {
		String csv = HEADER
				+ "2026/11/14 9:12:00 오전 GMT+9,김모아,20241234,010,참석,\n"
				+ "어제 오후,이모아,20231515,010,불참,\n"
				+ ",박모아,20221111,010,참석,\n"
				+ "2026/11/14 9:40:00 오전 GMT+9,,20221111,010,참석,\n"
				+ "2026/11/14 9:50:00 오전 GMT+9,최모아,123456789012345678901,010,참석,\n";

		ParsedFormCsv parsed = FormCsvParser.parse(csv);

		assertThat(parsed.totalRows()).isEqualTo(5);
		assertThat(parsed.rows()).hasSize(1);
		assertThat(parsed.errors()).extracting(CsvRowError::rowNumber).containsExactly(3L, 4L, 5L, 6L);
		assertThat(parsed.errors().get(0).reason()).contains("타임스탬프 형식").contains("어제 오후");
		assertThat(parsed.errors().get(1).reason()).contains("타임스탬프가 비어");
		assertThat(parsed.errors().get(2).reason()).contains("이름이 비어");
		assertThat(parsed.errors().get(3).reason()).contains("학번이 20자");
	}

	@Test
	void 헤더보다_값이_많은_행은_실패로_두고_뒤의_빈_값은_무시한다() {
		String csv = "타임스탬프,이름\n"
				+ "2026/11/14 9:12:00 오전,김모아,,,\n"
				+ "2026/11/14 9:13:00 오전,이모아,덤\n";

		ParsedFormCsv parsed = FormCsvParser.parse(csv);

		assertThat(parsed.rows()).extracting(FormResponseRow::name).containsExactly("김모아");
		assertThat(parsed.errors()).singleElement().extracting(CsvRowError::rowNumber).isEqualTo(3L);
	}

	@Test
	void 값이_모자란_행은_빈_값으로_채운다() {
		ParsedFormCsv parsed = FormCsvParser.parse(HEADER + "2026/11/14 9:12:00 오전,김모아\n");

		FormResponseRow row = parsed.rows().get(0);
		assertThat(row.studentId()).isNull();
		assertThat(row.answers()).containsEntry("뒤풀이 참석 여부", "");
	}

	@Test
	void 빈_줄과_쉼표만_있는_줄은_세지_않는다() {
		String csv = HEADER + "\n,,,,,\n2026/11/14 9:12:00 오전,김모아,1,2,참석,\n\n";

		ParsedFormCsv parsed = FormCsvParser.parse(csv);

		assertThat(parsed.totalRows()).isEqualTo(1);
		assertThat(parsed.rows()).hasSize(1);
	}

	@Test
	void 헤더만_있으면_0건이다() {
		ParsedFormCsv parsed = FormCsvParser.parse(HEADER);

		assertThat(parsed.totalRows()).isZero();
		assertThat(parsed.rows()).isEmpty();
		assertThat(parsed.errors()).isEmpty();
	}

	@Test
	void 공백뿐인_파일은_EMPTY() {
		assertThatThrownBy(() -> FormCsvParser.parse("\n\n"))
				.isInstanceOf(CsvReadException.class)
				.extracting("reason").isEqualTo(CsvReadException.Reason.EMPTY);
	}

	@Test
	void 닫히지_않은_따옴표는_UNREADABLE() {
		assertThatThrownBy(() -> FormCsvParser.parse(HEADER + "2026/11/14 9:12:00 오전,\"김모아,1\n"))
				.isInstanceOf(CsvReadException.class)
				.extracting("reason").isEqualTo(CsvReadException.Reason.UNREADABLE);
	}

	@Test
	void CSV가_아닌_텍스트는_이름_열이_없어_COLUMN_MISSING() {
		assertThatThrownBy(() -> FormCsvParser.parse("{\"hello\": \"world\"}\n"))
				.isInstanceOf(CsvReadException.class)
				.extracting("reason").isEqualTo(CsvReadException.Reason.COLUMN_MISSING);
	}

	@Test
	void 연도가_뒤에_오는_형식은_파일_전체를_보고_일월_순서를_정한다() {
		String csv = "Timestamp,Name\n05/11/2026 9:00:00,A\n14/11/2026 9:00:00,B\n";

		ParsedFormCsv parsed = FormCsvParser.parse(csv);

		assertThat(parsed.rows()).extracting(FormResponseRow::appliedAt).containsExactly(
				LocalDateTime.of(2026, 11, 5, 9, 0),
				LocalDateTime.of(2026, 11, 14, 9, 0));
	}

	@Test
	void 같은_행은_같은_해시_다른_행은_다른_해시() {
		String row = "2026/11/14 9:12:00 오전 GMT+9,김모아,20241234,010,참석,\"줄\n바꿈\"\n";
		ParsedFormCsv first = FormCsvParser.parse(HEADER + row);
		ParsedFormCsv grown = FormCsvParser.parse(HEADER + row + "2026/11/14 9:13:00 오전 GMT+9,김모아,20241234,010,참석,\n");
		ParsedFormCsv crlf = FormCsvParser.parse((HEADER + row).replace("\n", "\r\n"));

		assertThat(grown.rows().get(0).rowHash()).isEqualTo(first.rows().get(0).rowHash());
		assertThat(crlf.rows().get(0).rowHash()).isEqualTo(first.rows().get(0).rowHash());
		assertThat(grown.rows().get(1).rowHash()).isNotEqualTo(first.rows().get(0).rowHash());
	}

	@Test
	void UTF8과_CP949로_저장한_같은_파일은_같은_해시() {
		String csv = HEADER + "2026. 11. 14 오전 9:12:00,김모아,20241234,010,참석,좋아요\n";

		String fromUtf8 = FormCsvParser.parse(CsvDecoder.decode(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
				.rows().get(0).rowHash();
		String fromCp949 = FormCsvParser.parse(CsvDecoder.decode(csv.getBytes(CsvDecoder.MS949)))
				.rows().get(0).rowHash();

		assertThat(fromCp949).isEqualTo(fromUtf8);
	}
}
