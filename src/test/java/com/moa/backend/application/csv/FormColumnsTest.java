package com.moa.backend.application.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class FormColumnsTest {

	@Test
	void 기본_헤더를_찾고_나머지는_헤더_순서로_answers_키가_된다() {
		FormColumns columns = FormColumns.resolve(List.of("타임스탬프", "이름", "학번", "연락처", "뒤풀이 참석 여부", "하고 싶은 말"));

		assertThat(columns.timestamp()).isZero();
		assertThat(columns.name()).isEqualTo(1);
		assertThat(columns.studentId()).isEqualTo(2);
		assertThat(columns.contact()).isEqualTo(3);
		assertThat(columns.answerKeys()).containsExactly(
				java.util.Map.entry(4, "뒤풀이 참석 여부"),
				java.util.Map.entry(5, "하고 싶은 말"));
	}

	@Test
	void 문장형_헤더도_포함_여부로_찾는다() {
		FormColumns columns = FormColumns.resolve(List.of(
				"Timestamp", "이름을 적어 주세요", "학번 (예: 20241234)", "연락 가능한 휴대폰 번호", "뒤풀이에 참석하시나요?"));

		assertThat(columns.timestamp()).isZero();
		assertThat(columns.name()).isEqualTo(1);
		assertThat(columns.studentId()).isEqualTo(2);
		assertThat(columns.contact()).isEqualTo(3);
	}

	@Test
	void 다른_대상을_가리키는_헤더는_건너뛴다() {
		FormColumns columns = FormColumns.resolve(List.of(
				"타임스탬프", "팀 이름", "추천인 이름", "보호자 연락처", "성함", "비상 연락처", "추천인 학번"));

		assertThat(columns.name()).isEqualTo(4);
		assertThat(columns.studentId()).isEqualTo(FormColumns.NONE);
		assertThat(columns.contact()).isEqualTo(FormColumns.NONE);
		assertThat(columns.answerKeys().values())
				.containsExactly("팀 이름", "추천인 이름", "보호자 연락처", "비상 연락처", "추천인 학번");
	}

	@Test
	void 영문_이름_열은_이름으로_쓰지_않는다() {
		FormColumns columns = FormColumns.resolve(List.of("타임스탬프", "이름 (영문)", "이름 (한글)"));

		assertThat(columns.name()).isEqualTo(2);
	}

	@Test
	void 정확히_같은_헤더가_문장형보다_우선한다() {
		FormColumns columns = FormColumns.resolve(List.of("타임스탬프", "이름을 부르는 방법", "이름"));

		assertThat(columns.name()).isEqualTo(2);
	}

	@Test
	void 연락처는_전화번호_계열이_이메일보다_우선한다() {
		FormColumns columns = FormColumns.resolve(List.of("타임스탬프", "이메일 주소", "이름", "전화번호"));

		assertThat(columns.contact()).isEqualTo(3);
		assertThat(columns.answerKeys().values()).containsExactly("이메일 주소");
	}

	@Test
	void 전화번호가_없으면_이메일을_연락처로_쓴다() {
		FormColumns columns = FormColumns.resolve(List.of("Timestamp", "Email Address", "Name"));

		assertThat(columns.name()).isEqualTo(2);
		assertThat(columns.contact()).isEqualTo(1);
	}

	@Test
	void 수신_동의_같은_문항은_연락처로_오인하지_않는다() {
		FormColumns columns = FormColumns.resolve(List.of("타임스탬프", "이름", "이메일 수신에 동의하시나요?", "전화 인터뷰 가능 여부"));

		assertThat(columns.contact()).isEqualTo(FormColumns.NONE);
	}

	@Test
	void 학번_연락처가_없어도_된다() {
		FormColumns columns = FormColumns.resolve(List.of("타임스탬프", "이름"));

		assertThat(columns.studentId()).isEqualTo(FormColumns.NONE);
		assertThat(columns.contact()).isEqualTo(FormColumns.NONE);
		assertThat(columns.answerKeys()).isEmpty();
	}

	@Test
	void BOM과_공백을_무시한다() {
		FormColumns columns = FormColumns.resolve(List.of("﻿타임스탬프", "  이 름 "));

		assertThat(columns.timestamp()).isZero();
		assertThat(columns.name()).isEqualTo(1);
		assertThat(columns.headers()).containsExactly("타임스탬프", "이 름");
	}

	@Test
	void 비어_있거나_중복된_헤더는_구분되는_키를_붙인다() {
		FormColumns columns = FormColumns.resolve(List.of("타임스탬프", "이름", "메모", "", "메모"));

		assertThat(columns.answerKeys().values()).containsExactly("메모", "열 4", "메모 (2)");
	}

	@Test
	void 이름_열이_없으면_COLUMN_MISSING() {
		assertThatThrownBy(() -> FormColumns.resolve(List.of("타임스탬프", "닉네임", "학번")))
				.isInstanceOf(CsvReadException.class)
				.hasMessageContaining("이름 열")
				.extracting("reason").isEqualTo(CsvReadException.Reason.COLUMN_MISSING);
	}

	@Test
	void 타임스탬프_열이_없으면_COLUMN_MISSING() {
		assertThatThrownBy(() -> FormColumns.resolve(List.of("이름", "학번")))
				.isInstanceOf(CsvReadException.class)
				.hasMessageContaining("타임스탬프 열")
				.extracting("reason").isEqualTo(CsvReadException.Reason.COLUMN_MISSING);
	}
}
