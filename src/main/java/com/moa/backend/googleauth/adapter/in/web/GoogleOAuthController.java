package com.moa.backend.googleauth.adapter.in.web;

import com.moa.backend.googleauth.application.port.in.CompleteGoogleAuthorizationUseCase;
import com.moa.backend.googleauth.application.port.in.StartGoogleAuthorizationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자가 브라우저로 Google 계정을 연결하는 입구.
 * 1) GET /oauth/google/authorize → Google 동의 화면으로 이동
 * 2) Google이 GET /oauth/google/callback?code=...&state=... 로 돌려보냄 → 토큰 저장
 */
@Tag(name = "Google OAuth", description = "Google 계정 연결. 브라우저에서 /oauth/google/authorize 를 직접 여세요 (Swagger에서는 리다이렉트를 따라가지 못함).")
@RestController
@RequestMapping("/oauth/google")
public class GoogleOAuthController {

    private static final MediaType TEXT_UTF8 = new MediaType(MediaType.TEXT_PLAIN, java.nio.charset.StandardCharsets.UTF_8);

    private final StartGoogleAuthorizationUseCase startUseCase;
    private final CompleteGoogleAuthorizationUseCase completeUseCase;
    private final GoogleOAuthWebProperties properties;

    public GoogleOAuthController(StartGoogleAuthorizationUseCase startUseCase,
                                 CompleteGoogleAuthorizationUseCase completeUseCase,
                                 GoogleOAuthWebProperties properties) {
        this.startUseCase = startUseCase;
        this.completeUseCase = completeUseCase;
        this.properties = properties;
    }

    @Operation(summary = "Google 계정 연결 시작", description = "Google 동의 화면으로 302 리다이렉트. localhost에서만 허용.")
    @GetMapping("/authorize")
    public ResponseEntity<String> authorize(@Parameter(hidden = true) HttpServletRequest request) {
        if (!isAllowed(request)) {
            return forbidden();
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(startUseCase.start())).build();
    }

    @Operation(summary = "Google 리디렉션 콜백", description = "Google이 호출한다. 직접 부를 일 없음.")
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@Parameter(hidden = true) HttpServletRequest request,
                                           @RequestParam(required = false) String code,
                                           @RequestParam(required = false) String state,
                                           @RequestParam(required = false) String error) {
        if (!isAllowed(request)) {
            return forbidden();
        }
        if (error != null) {
            return text(HttpStatus.BAD_REQUEST, "Google 계정 연결이 취소되었습니다: " + error);
        }
        completeUseCase.complete(code, state);
        return text(HttpStatus.OK, "Google 계정이 연결되었습니다. 이 창을 닫아도 됩니다.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return text(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> failed(IllegalStateException e) {
        return text(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    private boolean isAllowed(HttpServletRequest request) {
        if (properties.allowRemote()) {
            return true;
        }
        try {
            return InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static ResponseEntity<String> forbidden() {
        return text(HttpStatus.FORBIDDEN, "Google 계정 연결은 서버 PC(localhost)에서만 할 수 있습니다.");
    }

    private static ResponseEntity<String> text(HttpStatus status, String body) {
        return ResponseEntity.status(status).contentType(TEXT_UTF8).body(body);
    }
}
