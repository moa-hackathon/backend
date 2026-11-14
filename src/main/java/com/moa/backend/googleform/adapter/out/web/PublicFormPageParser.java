package com.moa.backend.googleform.adapter.out.web;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.backend.googleform.domain.FormLinkException;
import com.moa.backend.googleform.domain.PublicForm;
import com.moa.backend.googleform.domain.PublicForm.PublicFormItem;
import com.moa.backend.googleform.domain.PublicForm.PublicFormItemType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 공개 구글폼 응답 페이지(HTML)에서 문항을 읽는다.
 *
 * <p>응답 페이지에는 {@code var FB_PUBLIC_LOAD_DATA_ = [...];} 형태로 폼 정의가 JSON 배열로 들어 있다.
 * 공식 API가 아니라서 위치가 바뀔 수 있으므로, 2026-11 실제 공개 폼에서 확인한 아래 자리만 쓴다.
 * <pre>
 * data[1][1]  문항 목록
 * data[1][8]  폼 제목 (없으면 data[3] 파일 이름)
 * 문항 item: [0]=id, [1]=제목, [2]=설명, [3]=유형 번호, [4]=응답 칸 목록 [[entryId, 보기, 필수(0/1), ...], ...]
 * </pre>
 * 응답 칸이 없는 항목(섹션 제목, 설명 글, 이미지, 동영상, 페이지 나누기)은 문항이 아니므로 뺀다.
 * 그리드처럼 칸이 여러 개면 하나라도 필수일 때 필수로 본다.
 */
final class PublicFormPageParser {

    static final String DATA_MARKER = "FB_PUBLIC_LOAD_DATA_";
    static final String NOT_A_FORM =
            "구글폼 문항을 읽을 수 없어요. 구글폼의 [보내기] > 링크에서 복사한 응답자용 링크인지 확인해 주세요.";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PublicFormPageParser() {
    }

    static PublicForm parse(String html) {
        JsonNode data = extractData(html);
        JsonNode formInfo = data.path(1);
        JsonNode rawItems = formInfo.path(1);
        if (!formInfo.isArray() || !(rawItems.isArray() || rawItems.isNull() || rawItems.isMissingNode())) {
            throw new FormLinkException(NOT_A_FORM);
        }

        List<PublicFormItem> items = new ArrayList<>();
        for (JsonNode item : rawItems) {
            PublicFormItem parsed = parseItem(item);
            if (parsed != null) {
                items.add(parsed);
            }
        }
        return new PublicForm(title(data), items);
    }

    private static JsonNode extractData(String html) {
        if (html == null) {
            throw new FormLinkException(NOT_A_FORM);
        }
        int marker = html.indexOf(DATA_MARKER);
        if (marker < 0) {
            throw new FormLinkException(NOT_A_FORM);
        }
        int start = html.indexOf('[', marker);
        int equals = html.indexOf('=', marker);
        if (start < 0 || equals < 0 || equals > start) {
            throw new FormLinkException(NOT_A_FORM);
        }
        // 배열 하나만 읽고 멈춘다. 뒤에 이어지는 ";</script>..."는 보지 않는다.
        try (JsonParser parser = OBJECT_MAPPER.getFactory().createParser(html.substring(start))) {
            JsonNode node = OBJECT_MAPPER.readTree(parser);
            if (node == null || !node.isArray()) {
                throw new FormLinkException(NOT_A_FORM);
            }
            return node;
        } catch (IOException e) {
            throw new FormLinkException(NOT_A_FORM, e);
        }
    }

    private static PublicFormItem parseItem(JsonNode item) {
        if (!item.isArray()) {
            return null;
        }
        JsonNode entries = item.path(4);
        if (!entries.isArray() || entries.isEmpty()) {
            return null;
        }
        String title = text(item.path(1));
        if (title == null) {
            return null;
        }
        boolean required = false;
        for (JsonNode entry : entries) {
            if (entry.path(2).asInt(0) == 1) {
                required = true;
                break;
            }
        }
        PublicFormItemType type = item.path(3).canConvertToInt()
                ? PublicFormItemType.fromCode(item.path(3).asInt())
                : PublicFormItemType.OTHER;
        return new PublicFormItem(title, required, type);
    }

    private static String title(JsonNode data) {
        String title = text(data.path(1).path(8));
        return title != null ? title : text(data.path(3));
    }

    private static String text(JsonNode node) {
        if (!node.isTextual()) {
            return null;
        }
        String value = node.asText().strip();
        return value.isEmpty() ? null : value;
    }
}
