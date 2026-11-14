package com.moa.backend.googleform.adapter.out.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moa.backend.googleauth.application.port.in.GetGoogleAccessTokenUseCase;
import com.moa.backend.googleform.application.port.out.GoogleFormPort;
import com.moa.backend.googleform.domain.CreatedForm;
import com.moa.backend.googleform.domain.FormDraft;
import com.moa.backend.googleform.domain.FormQuestion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Google Forms API 어댑터.
 * 1) 게시 안 된 상태로 폼 생성 → 2) 설명·질문 추가(batchUpdate) → 3) 게시.
 * 2, 3단계에서 실패해도 이미 만든 폼을 잃지 않도록 편집 링크를 오류 메시지에 넣는다.
 */
@Component
public class GoogleFormsApiAdapter implements GoogleFormPort {

    static final String BASE_URL = "https://forms.googleapis.com/v1";
    private static final int MAX_ERROR_BODY = 500;

    private final RestClient restClient;
    private final GetGoogleAccessTokenUseCase accessTokenUseCase;

    public GoogleFormsApiAdapter(RestClient.Builder restClientBuilder, GetGoogleAccessTokenUseCase accessTokenUseCase) {
        this.restClient = restClientBuilder.build();
        this.accessTokenUseCase = accessTokenUseCase;
    }

    @Override
    public CreatedForm create(FormDraft draft) {
        String token = accessTokenUseCase.accessToken();

        FormResponse created = call("폼 생성", () -> restClient.post()
                .uri(BASE_URL + "/forms?unpublished=true")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("info", Map.of("title", draft.title(), "documentTitle", draft.title())))
                .retrieve()
                .body(FormResponse.class));
        if (created == null || created.formId() == null) {
            throw new IllegalStateException("Google Forms 응답에 formId가 없습니다.");
        }
        String formId = created.formId();
        String editUrl = "https://docs.google.com/forms/d/%s/edit".formatted(formId);

        try {
            List<Map<String, Object>> requests = buildRequests(draft);
            if (!requests.isEmpty()) {
                call("질문 추가", () -> restClient.post()
                        .uri(BASE_URL + "/forms/{formId}:batchUpdate", formId)
                        .headers(h -> h.setBearerAuth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("requests", requests))
                        .retrieve()
                        .toBodilessEntity());
            }
            call("게시", () -> restClient.post()
                    .uri(BASE_URL + "/forms/{formId}:setPublishSettings", formId)
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "publishSettings", Map.of("publishState",
                                    Map.of("isPublished", true, "isAcceptingResponses", true)),
                            "updateMask", "publishState"))
                    .retrieve()
                    .toBodilessEntity());
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    "폼은 만들어졌지만 완성하지 못했습니다(게시 안 됨). 편집 링크: %s / 원인: %s".formatted(editUrl, e.getMessage()), e);
        }

        String responderUrl = created.responderUri() != null
                ? created.responderUri()
                : "https://docs.google.com/forms/d/%s/viewform".formatted(formId);
        return new CreatedForm(formId, responderUrl, editUrl);
    }

    private static List<Map<String, Object>> buildRequests(FormDraft draft) {
        List<Map<String, Object>> requests = new ArrayList<>();
        if (draft.hasDescription()) {
            requests.add(Map.of("updateFormInfo", Map.of(
                    "info", Map.of("description", draft.description()),
                    "updateMask", "description")));
        }
        List<FormQuestion> questions = draft.questions();
        for (int i = 0; i < questions.size(); i++) {
            requests.add(Map.of("createItem", Map.of(
                    "item", toItem(questions.get(i)),
                    "location", Map.of("index", i))));
        }
        return requests;
    }

    private static Map<String, Object> toItem(FormQuestion q) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("required", q.required());
        switch (q.type()) {
            case SHORT_ANSWER -> question.put("textQuestion", Map.of("paragraph", false));
            case PARAGRAPH -> question.put("textQuestion", Map.of("paragraph", true));
            case MULTIPLE_CHOICE -> question.put("choiceQuestion", choice("RADIO", q.options()));
            case CHECKBOX -> question.put("choiceQuestion", choice("CHECKBOX", q.options()));
            case DROPDOWN -> question.put("choiceQuestion", choice("DROP_DOWN", q.options()));
            case LINEAR_SCALE -> {
                Map<String, Object> scale = new LinkedHashMap<>();
                scale.put("low", q.scaleLow());
                scale.put("high", q.scaleHigh());
                if (q.scaleLowLabel() != null) {
                    scale.put("lowLabel", q.scaleLowLabel());
                }
                if (q.scaleHighLabel() != null) {
                    scale.put("highLabel", q.scaleHighLabel());
                }
                question.put("scaleQuestion", scale);
            }
            case DATE -> question.put("dateQuestion", Map.of("includeYear", true, "includeTime", false));
            case TIME -> question.put("timeQuestion", Map.of("duration", false));
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", q.title());
        if (q.description() != null && !q.description().isBlank()) {
            item.put("description", q.description());
        }
        item.put("questionItem", Map.of("question", question));
        return item;
    }

    private static Map<String, Object> choice(String type, List<String> options) {
        return Map.of("type", type, "options", options.stream().map(o -> Map.of("value", o)).toList());
    }

    private static <T> T call(String step, java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            if (body.length() > MAX_ERROR_BODY) {
                body = body.substring(0, MAX_ERROR_BODY) + "...";
            }
            String hint = switch (HttpStatus.resolve(e.getStatusCode().value())) {
                case UNAUTHORIZED -> "Google 인증이 거부되었습니다. /oauth/google/authorize 로 다시 연결하세요";
                case FORBIDDEN -> "권한이 없습니다. GCP 프로젝트에서 Google Forms API가 켜져 있는지, 스코프를 허용했는지 확인하세요";
                case BAD_REQUEST -> "요청 내용이 잘못되었습니다";
                case TOO_MANY_REQUESTS -> "요청 한도를 넘었습니다. 잠시 후 다시 시도하세요";
                case null, default -> "Google Forms API 오류";
            };
            throw new IllegalStateException(
                    "Google 폼 %s 실패 (HTTP %d): %s. 응답: %s".formatted(step, e.getStatusCode().value(), hint, body), e);
        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "Google 폼 %s 요청에 실패했습니다: %s".formatted(step, e.getClass().getSimpleName()), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FormResponse(String formId, String responderUri) {
    }
}
