package com.moa.backend.googleform.adapter.out.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moa.backend.googleform.domain.FormLinkException;
import com.moa.backend.googleform.domain.GoogleFormLink;
import com.moa.backend.googleform.domain.PublicForm;
import com.moa.backend.googleform.domain.PublicForm.PublicFormItem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 실제 네트워크 대신 HttpClient를 흉내 내어 리다이렉트/오류 처리를 확인한다. */
class PublicFormPageAdapterTest {

    private static final String SHORT_LINK = "https://forms.gle/AbCdEf123";
    private static final String VIEW_FORM = "https://docs.google.com/forms/d/e/1FAIpQLSabc/viewform?usp=send_form";

    private HttpClient httpClient;
    private PublicFormPageAdapter adapter;
    private final List<HttpResponse<InputStream>> queue = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        httpClient = mock(HttpClient.class);
        adapter = new PublicFormPageAdapter(httpClient);
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> queue.remove(0));
    }

    @Test
    void 짧은_링크의_리다이렉트를_따라가_문항을_읽는다() throws Exception {
        queue.add(redirect(VIEW_FORM));
        queue.add(page(PublicFormPageParserTest.fixture()));

        PublicForm form = adapter.read(GoogleFormLink.parse(SHORT_LINK));

        assertThat(form.title()).isEqualTo("모아 네트워킹 행사 신청");
        assertThat(form.items()).extracting(PublicFormItem::title)
                .containsExactly("이름", "학번", "연락처", "뒤풀이 참석 여부", "행사 만족도 (항목별)");
        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(), any());
        assertThat(requests.getAllValues()).extracting(HttpRequest::uri)
                .containsExactly(URI.create(SHORT_LINK), URI.create(VIEW_FORM));
        assertThat(requests.getAllValues().get(0).timeout()).contains(PublicFormPageAdapter.REQUEST_TIMEOUT);
    }

    @Test
    void 상대_경로_리다이렉트도_같은_호스트로_따라간다() throws Exception {
        queue.add(redirect("/forms/d/e/1FAIpQLSabc/viewform"));
        queue.add(page(PublicFormPageParserTest.fixture()));

        PublicForm form = adapter.read(GoogleFormLink.parse("https://docs.google.com/forms/d/e/1FAIpQLSabc/viewform?usp=sf_link"));

        assertThat(form.items()).hasSize(5);
    }

    @Test
    void 로그인_페이지로_가면_로그인이_필요한_폼() {
        queue.add(redirect("https://accounts.google.com/v3/signin/identifier?continue=https://docs.google.com/forms"));

        assertThatThrownBy(() -> adapter.read(GoogleFormLink.parse(VIEW_FORM)))
                .isInstanceOf(FormLinkException.class)
                .hasMessage(PublicFormPageAdapter.LOGIN_REQUIRED);
    }

    @Test
    void 응답을_닫은_폼은_closedform으로_간다() throws Exception {
        queue.add(redirect("https://docs.google.com/forms/d/1eEh481yAVmo/closedform"));

        assertThatThrownBy(() -> adapter.read(GoogleFormLink.parse(VIEW_FORM)))
                .isInstanceOf(FormLinkException.class)
                .hasMessage(PublicFormPageAdapter.CLOSED);
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    void 허용되지_않은_호스트로_리다이렉트되면_따라가지_않는다() throws Exception {
        queue.add(redirect("https://169.254.169.254/latest/meta-data"));

        assertThatThrownBy(() -> adapter.read(GoogleFormLink.parse(SHORT_LINK)))
                .isInstanceOf(FormLinkException.class)
                .hasMessage(PublicFormPageAdapter.OFF_SITE);
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    void http로_내려가는_리다이렉트는_따라가지_않는다() throws Exception {
        queue.add(redirect("http://docs.google.com/forms/d/e/abc/viewform"));

        assertThatThrownBy(() -> adapter.read(GoogleFormLink.parse(SHORT_LINK)))
                .hasMessage(PublicFormPageAdapter.OFF_SITE);
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    void 리다이렉트가_너무_많으면_멈춘다() throws Exception {
        for (int i = 0; i <= PublicFormPageAdapter.MAX_REDIRECTS; i++) {
            queue.add(redirect(SHORT_LINK));
        }

        assertThatThrownBy(() -> adapter.read(GoogleFormLink.parse(SHORT_LINK)))
                .isInstanceOf(FormLinkException.class);
        verify(httpClient, times(PublicFormPageAdapter.MAX_REDIRECTS + 1)).send(any(HttpRequest.class), any());
    }

    @Test
    void 없는_폼은_찾을_수_없다고_알린다() {
        queue.add(response(404, Map.of(), ""));

        assertThatThrownBy(() -> adapter.read(GoogleFormLink.parse(VIEW_FORM)))
                .hasMessage(PublicFormPageAdapter.NOT_FOUND);
    }

    @Test
    void 폼_데이터가_없는_페이지는_폼이_아니다() {
        queue.add(page("<html><body>hello</body></html>"));

        assertThatThrownBy(() -> adapter.read(GoogleFormLink.parse(VIEW_FORM)))
                .hasMessage(PublicFormPageParser.NOT_A_FORM);
    }

    @Test
    void 너무_큰_응답은_읽다가_멈춘다() {
        queue.add(page("a".repeat(PublicFormPageAdapter.MAX_BYTES + 1)));

        assertThatThrownBy(() -> adapter.read(GoogleFormLink.parse(VIEW_FORM)))
                .isInstanceOf(FormLinkException.class)
                .hasMessage(PublicFormPageParser.NOT_A_FORM);
    }

    @Test
    void 시간_초과와_네트워크_오류는_접속_실패로_알린다() throws Exception {
        HttpClient failing = mock(HttpClient.class);
        doThrow(new HttpTimeoutException("timeout")).when(failing).send(any(HttpRequest.class), any());
        assertThatThrownBy(() -> new PublicFormPageAdapter(failing).read(GoogleFormLink.parse(VIEW_FORM)))
                .isInstanceOf(FormLinkException.class)
                .hasMessageContaining("너무 늦어요");

        HttpClient broken = mock(HttpClient.class);
        doThrow(new IOException("connection reset")).when(broken).send(any(HttpRequest.class), any());
        assertThatThrownBy(() -> new PublicFormPageAdapter(broken).read(GoogleFormLink.parse(VIEW_FORM)))
                .hasMessage(PublicFormPageAdapter.UNREACHABLE);
    }

    private static HttpResponse<InputStream> redirect(String location) {
        return response(302, Map.of("Location", List.of(location)), "");
    }

    private static HttpResponse<InputStream> page(String html) {
        return response(200, Map.of("Content-Type", List.of("text/html; charset=utf-8")), html);
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<InputStream> response(int status, Map<String, List<String>> headers, String body) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        doReturn(status).when(response).statusCode();
        doReturn(HttpHeaders.of(headers, (name, value) -> true)).when(response).headers();
        doReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))).when(response).body();
        return response;
    }
}
