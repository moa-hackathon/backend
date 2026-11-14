package com.moa.backend.dashboard.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ExcelDownloadTest {

	@Test
	void 한글_파일명은_RFC5987_filename_별표에_UTF8로_인코딩한다() {
		ExcelDownload download = ExcelDownload.forEvent(7L, "모아 네트워킹 행사", new byte[0]);

		String header = download.contentDisposition();

		assertThat(download.fileName()).isEqualTo("모아 네트워킹 행사_신청자.xlsx");
		assertThat(header).startsWith("attachment; filename=\"event-7-applications.xlsx\"; filename*=UTF-8''");
		String encoded = header.substring(header.indexOf("UTF-8''") + "UTF-8''".length());
		assertThat(encoded).doesNotContain(" ", "+").matches("[A-Za-z0-9%._\\-]+");
		assertThat(URLDecoder.decode(encoded, StandardCharsets.UTF_8)).isEqualTo("모아 네트워킹 행사_신청자.xlsx");
	}

	@Test
	void 파일명에_쓸_수_없는_문자는_바꾼다() {
		assertThat(ExcelDownload.forEvent(1L, "A/B: \"C\"?", new byte[0]).fileName()).isEqualTo("A_B_ _C___신청자.xlsx");
		assertThat(ExcelDownload.forEvent(1L, "  ", new byte[0]).fileName()).isEqualTo("행사_신청자.xlsx");
	}
}
