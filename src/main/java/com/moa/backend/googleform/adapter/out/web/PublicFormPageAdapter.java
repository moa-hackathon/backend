package com.moa.backend.googleform.adapter.out.web;

import com.moa.backend.googleform.application.port.out.PublicFormPagePort;
import com.moa.backend.googleform.domain.FormLinkException;
import com.moa.backend.googleform.domain.GoogleFormLink;
import com.moa.backend.googleform.domain.PublicForm;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 로그인 없이 열리는 구글폼 응답 페이지를 내려받아 문항을 읽는 어댑터.
 *
 * <p>사용자가 준 주소를 서버가 여는 것이므로 SSRF를 막는다.
 * <ul>
 *   <li>https + forms.gle / docs.google.com 만 요청한다 ({@link GoogleFormLink#isAllowedLocation(URI)}).</li>
 *   <li>리다이렉트는 HttpClient에 맡기지 않고 직접 따라가며, 매번 옮겨 갈 주소를 다시 검사한다 (최대 {@value #MAX_REDIRECTS}번).</li>
 *   <li>연결/응답 시간 제한과 본문 크기 상한({@value #MAX_BYTES}바이트)을 둔다.</li>
 * </ul>
 * 비공개 폼은 accounts.google.com 로그인 페이지로, 응답을 닫은 폼은 /closedform 으로 리다이렉트된다.
 */
@Slf4j
@Component
public class PublicFormPageAdapter implements PublicFormPagePort {

    static final int MAX_REDIRECTS = 5;
    /** 실제 공개 폼 페이지는 100~300KB 정도다. */
    static final int MAX_BYTES = 3 * 1024 * 1024;
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    static final String LOGIN_HOST = "accounts.google.com";
    static final String LOGIN_REQUIRED =
            "로그인해야 열리는 폼이에요. 구글폼 [설정] > [응답]에서 '로그인 필요'와 '조직 내 사용자로 제한'을 꺼 주세요.";
    static final String CLOSED =
            "응답을 받지 않고 있는 폼이에요. 구글폼 [응답] 탭에서 '응답 받기'를 켜 주세요.";
    static final String NOT_FOUND = "폼을 찾을 수 없어요. 링크가 정확한지 확인해 주세요.";
    static final String OFF_SITE = "구글폼이 아닌 주소로 이동하는 링크예요. 구글폼 응답자용 링크를 붙여넣어 주세요.";
    static final String UNREACHABLE = "구글폼에 접속하지 못했어요. 잠시 후 다시 시도해 주세요.";

    private final HttpClient httpClient;

    public PublicFormPageAdapter() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    PublicFormPageAdapter(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public PublicForm read(GoogleFormLink link) {
        URI current = link.uri();
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            if (!GoogleFormLink.isAllowedLocation(current)) {
                throw new FormLinkException(OFF_SITE);
            }
            if (isClosedForm(current)) {
                throw new FormLinkException(CLOSED);
            }
            HttpResponse<InputStream> response = send(current);
            int status = response.statusCode();
            try (InputStream body = response.body()) {
                if (isRedirect(status)) {
                    current = redirectTarget(current, response);
                    continue;
                }
                if (status == 200) {
                    return PublicFormPageParser.parse(readLimited(body));
                }
                throw new FormLinkException(messageFor(status));
            } catch (IOException e) {
                throw new FormLinkException(UNREACHABLE, e);
            }
        }
        throw new FormLinkException(OFF_SITE);
    }

    private HttpResponse<InputStream> send(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/html")
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (HttpTimeoutException e) {
            throw new FormLinkException("구글폼 응답이 너무 늦어요. 잠시 후 다시 시도해 주세요.", e);
        } catch (IOException e) {
            log.warn("구글폼 요청 실패: {} ({})", uri.getHost(), e.getClass().getSimpleName());
            throw new FormLinkException(UNREACHABLE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FormLinkException(UNREACHABLE, e);
        }
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static URI redirectTarget(URI current, HttpResponse<?> response) {
        Optional<String> location = response.headers().firstValue("Location");
        if (location.isEmpty() || location.get().isBlank()) {
            throw new FormLinkException(UNREACHABLE);
        }
        URI target;
        try {
            target = current.resolve(location.get().trim());
        } catch (IllegalArgumentException e) {
            throw new FormLinkException(OFF_SITE);
        }
        String host = target.getHost() == null ? "" : target.getHost().toLowerCase(Locale.ROOT);
        if (LOGIN_HOST.equals(host)) {
            // 로그인 페이지는 열지 않는다. 비공개 폼이라는 뜻이다.
            throw new FormLinkException(LOGIN_REQUIRED);
        }
        return target;
    }

    private static boolean isClosedForm(URI uri) {
        String path = uri.getPath();
        return path != null && path.endsWith("/closedform");
    }

    private static String messageFor(int status) {
        if (status == 400 || status == 404 || status == 410) {
            return NOT_FOUND;
        }
        if (status == 401 || status == 403) {
            return LOGIN_REQUIRED;
        }
        return UNREACHABLE;
    }

    private static String readLimited(InputStream body) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = body.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BYTES) {
                throw new FormLinkException(PublicFormPageParser.NOT_A_FORM);
            }
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
