package com.moa.backend.application.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CsvDecoderTest {

	private static final String CSV = "타임스탬프,이름,학번\n2026/11/14 9:12:00 오전 GMT+9,김모아,20241234\n";

	@Test
	void UTF8을_그대로_읽는다() {
		assertThat(CsvDecoder.decode(CSV.getBytes(StandardCharsets.UTF_8))).isEqualTo(CSV);
	}

	@Test
	void UTF8_BOM을_뗀다() {
		byte[] body = CSV.getBytes(StandardCharsets.UTF_8);
		byte[] withBom = new byte[body.length + 3];
		withBom[0] = (byte) 0xEF;
		withBom[1] = (byte) 0xBB;
		withBom[2] = (byte) 0xBF;
		System.arraycopy(body, 0, withBom, 3, body.length);

		String decoded = CsvDecoder.decode(withBom);

		assertThat(decoded).isEqualTo(CSV);
		assertThat(decoded.charAt(0)).isEqualTo('타');
	}

	@Test
	void UTF8이_아니면_CP949로_읽는다() {
		byte[] cp949 = CSV.getBytes(CsvDecoder.MS949);

		assertThat(CsvDecoder.decode(cp949)).isEqualTo(CSV);
	}

	@Test
	void CP949_확장_한글도_읽는다() {
		String text = "이름\n똠방각하,쀍\n";

		assertThat(CsvDecoder.decode(text.getBytes(CsvDecoder.MS949))).isEqualTo(text);
	}

	@Test
	void 빈_파일은_EMPTY() {
		assertThatThrownBy(() -> CsvDecoder.decode(new byte[0]))
				.isInstanceOf(CsvReadException.class)
				.extracting("reason").isEqualTo(CsvReadException.Reason.EMPTY);
	}

	@Test
	void BOM만_있는_파일은_EMPTY() {
		assertThatThrownBy(() -> CsvDecoder.decode(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}))
				.isInstanceOf(CsvReadException.class)
				.extracting("reason").isEqualTo(CsvReadException.Reason.EMPTY);
	}

	@Test
	void NUL_바이트가_있는_바이너리는_UNREADABLE() {
		byte[] xlsxLike = {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};

		assertThatThrownBy(() -> CsvDecoder.decode(xlsxLike))
				.isInstanceOf(CsvReadException.class)
				.extracting("reason").isEqualTo(CsvReadException.Reason.UNREADABLE);
	}

	@Test
	void UTF8도_CP949도_아니면_UNREADABLE() {
		byte[] invalid = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

		assertThatThrownBy(() -> CsvDecoder.decode(invalid))
				.isInstanceOf(CsvReadException.class)
				.extracting("reason").isEqualTo(CsvReadException.Reason.UNREADABLE);
	}
}
