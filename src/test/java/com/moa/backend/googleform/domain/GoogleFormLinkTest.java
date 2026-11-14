package com.moa.backend.googleform.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GoogleFormLinkTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://forms.gle/AbCdEf123",
            "  https://forms.gle/AbCdEf123  ",
            "https://docs.google.com/forms/d/e/1FAIpQLSabc/viewform",
            "https://docs.google.com/forms/d/e/1FAIpQLSabc/viewform?usp=sf_link",
            "https://docs.google.com/forms/d/1eEh481yAVmo/viewform",
            "https://DOCS.google.com/forms/d/e/1FAIpQLSabc/viewform"
    })
    void 구글폼_응답자용_링크를_받는다(String raw) {
        GoogleFormLink link = GoogleFormLink.parse(raw);

        assertThat(link.url()).isEqualTo(raw.trim());
        assertThat(link.uri()).isEqualTo(URI.create(raw.trim()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "forms.gle/AbCdEf123",
            "http://forms.gle/AbCdEf123",
            "https://forms.gle/",
            "https://forms.gle",
            "https://forms.gle.evil.com/abc",
            "https://evil.com/forms.gle/abc",
            "https://docs.google.com/spreadsheets/d/abc",
            "https://docs.google.com/forms/d/abc/edit",
            "https://docs.google.com:8443/forms/d/e/abc/viewform",
            "https://user@docs.google.com/forms/d/e/abc/viewform",
            "https://127.0.0.1/forms/d/e/abc/viewform",
            "https://localhost/forms/abc",
            "ftp://forms.gle/abc",
            "https://forms.gle/ab cd"
    })
    void 구글폼이_아닌_링크는_거절한다(String raw) {
        assertThatThrownBy(() -> GoogleFormLink.parse(raw)).isInstanceOf(FormLinkException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://docs.google.com/forms/d/e/abc/closedform",
            "https://forms.gle/abc"
    })
    void 허용된_리다이렉트_대상(String target) {
        assertThat(GoogleFormLink.isAllowedLocation(URI.create(target))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://docs.google.com/forms/d/e/abc/viewform",
            "https://accounts.google.com/ServiceLogin",
            "https://169.254.169.254/latest/meta-data",
            "https://docs.google.com.evil.com/forms",
            "/forms/d/e/abc/viewform"
    })
    void 허용되지_않는_리다이렉트_대상(String target) {
        assertThat(GoogleFormLink.isAllowedLocation(URI.create(target))).isFalse();
    }

    @Test
    void 너무_긴_링크는_거절한다() {
        String raw = "https://forms.gle/" + "a".repeat(500);

        assertThatThrownBy(() -> GoogleFormLink.parse(raw))
                .isInstanceOf(FormLinkException.class)
                .hasMessageContaining("너무 길어요");
    }
}
