package com.moa.backend.dashboard.export;

import com.moa.backend.application.entity.Application;
import com.moa.backend.dashboard.domain.ApplicationReview;
import com.moa.backend.dashboard.domain.ReviewedApplication;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 신청자 목록을 한 시트짜리 .xlsx로 만든다.
 * 열: 이름, 학번, 연락처, answers 문항들(등장 순), 신청 시각, 상태(한글 배지명).
 */
public final class ApplicationExcelWriter {

	public static final String SHEET_NAME = "신청자";
	private static final String DATE_TIME_FORMAT = "yyyy-mm-dd hh:mm";
	private static final int COLUMN_WIDTH = 18 * 256;

	private ApplicationExcelWriter() {
	}

	public static byte[] write(ApplicationReview review) {
		List<String> questions = review.questions();
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet(SHEET_NAME);
			CellStyle headerStyle = headerStyle(workbook);
			CellStyle dateTimeStyle = workbook.createCellStyle();
			dateTimeStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(DATE_TIME_FORMAT));

			List<String> headers = new ArrayList<>(List.of("이름", "학번", "연락처"));
			headers.addAll(questions);
			headers.add("신청 시각");
			headers.add("상태");
			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < headers.size(); i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers.get(i));
				cell.setCellStyle(headerStyle);
				sheet.setColumnWidth(i, COLUMN_WIDTH);
			}
			sheet.createFreezePane(0, 1);

			int rowIndex = 1;
			for (ReviewedApplication reviewed : review.rows()) {
				Application application = reviewed.application();
				Row row = sheet.createRow(rowIndex++);
				int col = 0;
				setText(row, col++, application.getName());
				setText(row, col++, application.getStudentId());
				setText(row, col++, application.getContact());
				Map<String, String> answers = application.getAnswers();
				for (String question : questions) {
					setText(row, col++, answers == null ? null : answers.get(question));
				}
				Cell appliedAt = row.createCell(col++);
				if (application.getAppliedAt() != null) {
					appliedAt.setCellValue(application.getAppliedAt());
					appliedAt.setCellStyle(dateTimeStyle);
				}
				setText(row, col, reviewed.displayStatus().getLabel());
			}

			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("엑셀 파일을 만들지 못했습니다.", e);
		}
	}

	private static CellStyle headerStyle(Workbook workbook) {
		Font bold = workbook.createFont();
		bold.setBold(true);
		CellStyle style = workbook.createCellStyle();
		style.setFont(bold);
		return style;
	}

	/** 학번·연락처의 앞자리 0이 사라지지 않도록 모든 값을 문자열 셀로 쓴다. */
	private static void setText(Row row, int col, String value) {
		Cell cell = row.createCell(col);
		if (value != null) {
			cell.setCellValue(value);
		}
	}
}
