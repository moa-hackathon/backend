package com.moa.backend.dashboard.export;

import static com.moa.backend.application.entity.ApplicationStatus.CANCELED;
import static com.moa.backend.application.entity.ApplicationStatus.CONFIRMED;
import static com.moa.backend.application.entity.ApplicationStatus.WAITING;
import static com.moa.backend.dashboard.domain.ApplicationFixture.BASE;
import static com.moa.backend.dashboard.domain.ApplicationFixture.application;
import static org.assertj.core.api.Assertions.assertThat;

import com.moa.backend.dashboard.domain.ApplicationReview;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ApplicationExcelWriterTest {

	@Test
	void 신청자_전체를_한_시트에_쓴다() throws IOException {
		Map<String, String> first = new LinkedHashMap<>();
		first.put("뒤풀이 참석 여부", "참석");
		Map<String, String> second = new LinkedHashMap<>();
		second.put("첫 참여", "예");
		second.put("뒤풀이 참석 여부", "불참");

		ApplicationReview review = ApplicationReview.of(List.of(
				application(1, "김모아", "0230015", "010-1234-5678", CONFIRMED, 0, first),
				application(2, "이모아", "20240001", null, WAITING, 10, second),
				application(3, "김모아", "0230015", "010-1234-5678", WAITING, 20, null),
				application(4, "박모아", "20250007", "010-2", CANCELED, 30, null)));

		byte[] bytes = ApplicationExcelWriter.write(review);

		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
			assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
			Sheet sheet = workbook.getSheet(ApplicationExcelWriter.SHEET_NAME);
			assertThat(texts(sheet.getRow(0)))
					.containsExactly("이름", "학번", "연락처", "뒤풀이 참석 여부", "첫 참여", "신청 시각", "상태");
			assertThat(sheet.getLastRowNum()).isEqualTo(4);

			Row row1 = sheet.getRow(1);
			assertThat(row1.getCell(0).getStringCellValue()).isEqualTo("김모아");
			assertThat(row1.getCell(1).getStringCellValue()).isEqualTo("0230015");
			assertThat(row1.getCell(2).getStringCellValue()).isEqualTo("010-1234-5678");
			assertThat(row1.getCell(3).getStringCellValue()).isEqualTo("참석");
			assertThat(row1.getCell(4).getStringCellValue()).isEmpty();
			assertThat(row1.getCell(5).getLocalDateTimeCellValue()).isEqualTo(BASE);
			assertThat(row1.getCell(6).getStringCellValue()).isEqualTo("확정");

			Row row2 = sheet.getRow(2);
			assertThat(row2.getCell(2).getStringCellValue()).isEmpty();
			assertThat(row2.getCell(3).getStringCellValue()).isEqualTo("불참");
			assertThat(row2.getCell(4).getStringCellValue()).isEqualTo("예");
			assertThat(row2.getCell(6).getStringCellValue()).isEqualTo("누락");

			assertThat(sheet.getRow(3).getCell(6).getStringCellValue()).isEqualTo("중복");
			assertThat(sheet.getRow(4).getCell(6).getStringCellValue()).isEqualTo("취소");
		}
	}

	@Test
	void 신청이_없으면_헤더만_쓴다() throws IOException {
		byte[] bytes = ApplicationExcelWriter.write(ApplicationReview.of(List.of()));

		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
			Sheet sheet = workbook.getSheetAt(0);
			assertThat(texts(sheet.getRow(0))).containsExactly("이름", "학번", "연락처", "신청 시각", "상태");
			assertThat(sheet.getLastRowNum()).isZero();
		}
	}

	private static List<String> texts(Row row) {
		List<String> texts = new ArrayList<>();
		for (Cell cell : row) {
			texts.add(cell.getStringCellValue());
		}
		return texts;
	}
}
