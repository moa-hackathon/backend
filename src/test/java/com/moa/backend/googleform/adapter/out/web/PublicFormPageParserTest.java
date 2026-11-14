package com.moa.backend.googleform.adapter.out.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.moa.backend.googleform.domain.FormLinkException;
import com.moa.backend.googleform.domain.PublicForm;
import com.moa.backend.googleform.domain.PublicForm.PublicFormItem;
import com.moa.backend.googleform.domain.PublicForm.PublicFormItemType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PublicFormPageParserTest {

    static String fixture() throws IOException {
        try (InputStream in = PublicFormPageParserTest.class.getResourceAsStream("/googleform/public-form.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void 공개_폼_페이지에서_응답_문항만_순서대로_읽는다() throws IOException {
        PublicForm form = PublicFormPageParser.parse(fixture());

        assertThat(form.title()).isEqualTo("모아 네트워킹 행사 신청");
        assertThat(form.items())
                .extracting(PublicFormItem::title, PublicFormItem::required, PublicFormItem::type)
                .containsExactly(
                        tuple("이름", true, PublicFormItemType.SHORT_ANSWER),
                        tuple("학번", true, PublicFormItemType.SHORT_ANSWER),
                        tuple("연락처", true, PublicFormItemType.SHORT_ANSWER),
                        tuple("뒤풀이 참석 여부", false, PublicFormItemType.MULTIPLE_CHOICE),
                        // 그리드는 행 하나라도 필수면 필수
                        tuple("행사 만족도 (항목별)", true, PublicFormItemType.GRID));
    }

    @Test
    void 폼_제목이_비어_있으면_파일_이름을_쓴다() {
        String html = "<script>var FB_PUBLIC_LOAD_DATA_ = [null,[null,[[1,\"이름\",null,0,[[2,null,1]]]],"
                + "null,null,null,null,null,null,\"\"],\"/forms\",\"신청서 파일\"];</script>";

        PublicForm form = PublicFormPageParser.parse(html);

        assertThat(form.title()).isEqualTo("신청서 파일");
        assertThat(form.items()).extracting(PublicFormItem::title).containsExactly("이름");
    }

    @Test
    void 문항이_없는_폼은_빈_목록() {
        String html = "<script>var FB_PUBLIC_LOAD_DATA_ = [null,[\"설명\",null,null,null,null,null,null,null,\"빈 폼\"]];</script>";

        PublicForm form = PublicFormPageParser.parse(html);

        assertThat(form.title()).isEqualTo("빈 폼");
        assertThat(form.items()).isEmpty();
    }

    @Test
    void 폼_데이터가_없는_페이지는_거절한다() {
        assertThatThrownBy(() -> PublicFormPageParser.parse("<html><title>Sign in</title></html>"))
                .isInstanceOf(FormLinkException.class)
                .hasMessage(PublicFormPageParser.NOT_A_FORM);
    }

    @Test
    void 깨진_데이터는_거절한다() {
        assertThatThrownBy(() -> PublicFormPageParser.parse("<script>var FB_PUBLIC_LOAD_DATA_ = [null,[\"a\",</script>"))
                .isInstanceOf(FormLinkException.class);
        assertThatThrownBy(() -> PublicFormPageParser.parse("<script>var FB_PUBLIC_LOAD_DATA_ = [null,\"x\"];</script>"))
                .isInstanceOf(FormLinkException.class);
    }
}
