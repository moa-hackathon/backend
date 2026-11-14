package com.moa.backend.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShareCodeGeneratorTest {

	@Test
	void URL에_쓸_수_있는_22자_코드를_매번_다르게_만든다() {
		ShareCodeGenerator generator = new ShareCodeGenerator();
		Set<String> codes = new HashSet<>();

		for (int i = 0; i < 1000; i++) {
			String code = generator.generate();
			assertThat(code).hasSize(22).matches("[A-Za-z0-9_-]+");
			codes.add(code);
		}

		assertThat(codes).hasSize(1000);
	}
}
