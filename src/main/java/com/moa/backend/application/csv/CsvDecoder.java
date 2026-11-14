package com.moa.backend.application.csv;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * 업로드된 바이트를 문자열로 바꾼다. UTF-8 BOM을 떼고 엄격한 UTF-8로 읽어 보고,
 * 실패하면 MS949(한국어 엑셀이 다시 저장한 CSV)로 읽는다.
 */
public final class CsvDecoder {

	public static final Charset MS949 = Charset.forName("MS949");

	private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

	private CsvDecoder() {
	}

	public static String decode(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			throw new CsvReadException(CsvReadException.Reason.EMPTY, "빈 파일입니다.");
		}
		int offset = startsWithBom(bytes) ? UTF8_BOM.length : 0;
		if (bytes.length == offset) {
			throw new CsvReadException(CsvReadException.Reason.EMPTY, "빈 파일입니다.");
		}
		// 엑셀(xlsx), UTF-16 텍스트 등 바이너리 파일은 NUL 바이트를 포함한다. 텍스트 CSV에는 나오지 않는다.
		for (int i = offset; i < bytes.length; i++) {
			if (bytes[i] == 0) {
				throw new CsvReadException(CsvReadException.Reason.UNREADABLE,
						"CSV 파일이 아닙니다. 구글폼 응답 탭에서 CSV로 내려받은 파일을 올려 주세요.");
			}
		}
		ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, bytes.length - offset);
		try {
			return strictDecode(buffer.duplicate(), StandardCharsets.UTF_8);
		} catch (CharacterCodingException notUtf8) {
			try {
				return strictDecode(buffer.duplicate(), MS949);
			} catch (CharacterCodingException notMs949) {
				throw new CsvReadException(CsvReadException.Reason.UNREADABLE,
						"파일 인코딩을 읽을 수 없습니다. UTF-8 CSV로 저장해 올려 주세요.");
			}
		}
	}

	private static String strictDecode(ByteBuffer buffer, Charset charset) throws CharacterCodingException {
		return charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(buffer)
				.toString();
	}

	private static boolean startsWithBom(byte[] bytes) {
		return bytes.length >= UTF8_BOM.length
				&& bytes[0] == UTF8_BOM[0]
				&& bytes[1] == UTF8_BOM[1]
				&& bytes[2] == UTF8_BOM[2];
	}
}
