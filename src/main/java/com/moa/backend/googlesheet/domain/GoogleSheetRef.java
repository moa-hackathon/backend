package com.moa.backend.googlesheet.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 가져올 Google 시트의 위치.
 *
 * @param spreadsheetId 시트 ID (링크의 /d/ 와 / 사이)
 * @param gid           탭 ID (링크의 #gid= 뒤 숫자). null이면 첫 번째 탭
 */
public record GoogleSheetRef(String spreadsheetId, String gid) {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{10,}");
    private static final Pattern GID = Pattern.compile("\\d+");
    private static final Pattern SHEET_URL = Pattern.compile(
            "^https://docs\\.google\\.com/spreadsheets/(?:u/\\d+/)?d/([A-Za-z0-9_-]{10,})(?:[/?#].*)?$");
    private static final Pattern GID_PARAM = Pattern.compile("[#?&]gid=(\\d+)");

    public GoogleSheetRef {
        if (spreadsheetId == null || !ID.matcher(spreadsheetId).matches()) {
            throw new IllegalArgumentException("Google 시트 ID 형식이 올바르지 않습니다.");
        }
        if (gid != null && !GID.matcher(gid).matches()) {
            throw new IllegalArgumentException("Google 시트 탭 ID(gid)는 숫자여야 합니다.");
        }
    }

    /**
     * 시트 공유 링크(또는 시트 ID만)에서 위치를 꺼낸다.
     * 예: https://docs.google.com/spreadsheets/d/{ID}/edit#gid=123
     */
    public static GoogleSheetRef parse(String urlOrId) {
        if (urlOrId == null || urlOrId.isBlank()) {
            throw new IllegalArgumentException("Google 시트 링크가 비어 있습니다.");
        }
        String input = urlOrId.strip();
        if (ID.matcher(input).matches()) {
            return new GoogleSheetRef(input, null);
        }
        if (input.contains("/spreadsheets/d/e/")) {
            throw new IllegalArgumentException(
                    "'웹에 게시' 링크(/d/e/...)는 지원하지 않습니다. 시트의 공유 링크(/d/{ID}/edit)를 넣어주세요.");
        }
        Matcher url = SHEET_URL.matcher(input);
        if (!url.matches()) {
            throw new IllegalArgumentException(
                    "Google 시트 링크 형식이 아닙니다. https://docs.google.com/spreadsheets/d/{ID}/edit 형태여야 합니다.");
        }
        Matcher gid = GID_PARAM.matcher(input);
        return new GoogleSheetRef(url.group(1), gid.find() ? gid.group(1) : null);
    }
}
