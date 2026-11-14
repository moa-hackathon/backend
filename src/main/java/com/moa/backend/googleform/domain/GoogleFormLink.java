package com.moa.backend.googleform.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * 운영진이 붙여넣은 구글폼 응답자용 링크.
 * 사용자가 준 주소를 서버가 직접 열기 때문에(SSRF) https + 허용된 호스트만 받는다.
 * 리다이렉트로 옮겨 간 주소도 {@link #isAllowedLocation(URI)}로 다시 검사한다.
 *
 * @param url 저장하고 공지에 넣을 원래 링크 (앞뒤 공백만 제거)
 * @param uri 실제로 요청할 주소
 */
public record GoogleFormLink(String url, URI uri) {

    public static final String SHORT_LINK_HOST = "forms.gle";
    public static final String DOCS_HOST = "docs.google.com";
    /** 리다이렉트로 거쳐 가도 되는 호스트. 로그인 페이지는 따로 판단하므로 여기 넣지 않는다. */
    static final Set<String> ALLOWED_HOSTS = Set.of(SHORT_LINK_HOST, DOCS_HOST);
    static final int MAX_LENGTH = 500;

    private static final String INVALID_FORMAT =
            "구글폼 링크가 아니에요. https://forms.gle/… 또는 https://docs.google.com/forms/… 형식의 링크를 붙여넣어 주세요.";

    public static GoogleFormLink parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new FormLinkException("구글폼 링크를 입력해 주세요.");
        }
        String url = raw.trim();
        if (url.length() > MAX_LENGTH) {
            throw new FormLinkException("링크가 너무 길어요(최대 %d자).".formatted(MAX_LENGTH));
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new FormLinkException(INVALID_FORMAT);
        }
        if (!isAllowedLocation(uri)) {
            throw new FormLinkException(INVALID_FORMAT);
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (SHORT_LINK_HOST.equals(host(uri)) && path.replace("/", "").isEmpty()) {
            throw new FormLinkException(INVALID_FORMAT);
        }
        if (DOCS_HOST.equals(host(uri))) {
            if (!path.startsWith("/forms/")) {
                throw new FormLinkException(INVALID_FORMAT);
            }
            if (path.endsWith("/edit")) {
                throw new FormLinkException(
                        "폼 편집 링크예요. 구글폼의 [보내기] > 링크에서 응답자용 링크를 복사해 붙여넣어 주세요.");
            }
        }
        return new GoogleFormLink(url, uri);
    }

    /** https, 기본 포트, 사용자 정보 없음, 허용된 호스트일 때만 true. 리다이렉트 대상 검사에도 쓴다. */
    public static boolean isAllowedLocation(URI uri) {
        if (uri == null || !uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        if (uri.getRawUserInfo() != null) {
            return false;
        }
        if (uri.getPort() != -1 && uri.getPort() != 443) {
            return false;
        }
        String host = host(uri);
        return host != null && ALLOWED_HOSTS.contains(host);
    }

    static String host(URI uri) {
        String host = uri.getHost();
        return host == null ? null : host.toLowerCase(Locale.ROOT);
    }
}
