package com.moa.backend.application.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RowHasherTest {

	@Test
	void 소문자_hex_64자이고_같은_입력이면_같다() {
		String hash = RowHasher.hash(List.of("2026/11/14 9:12:00", "김모아", "20241234"));

		assertThat(hash).matches("[0-9a-f]{64}");
		assertThat(RowHasher.hash(List.of("2026/11/14 9:12:00", "김모아", "20241234"))).isEqualTo(hash);
	}

	@Test
	void 알려진_값과_같다() {
		// printf 'a\x1fb' | shasum -a 256
		assertThat(RowHasher.hash(List.of("a", "b")))
				.isEqualTo("f04cdced9736a69da6103f08a4daaf8c485dd481217d218a1b4993c8c3968e13");
	}

	@Test
	void 필드_경계가_다르면_다르다() {
		assertThat(RowHasher.hash(List.of("ab", "c"))).isNotEqualTo(RowHasher.hash(List.of("a", "bc")));
		assertThat(RowHasher.hash(List.of("a", ""))).isNotEqualTo(RowHasher.hash(List.of("a")));
	}
}
