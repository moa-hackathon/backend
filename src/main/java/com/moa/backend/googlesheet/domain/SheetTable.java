package com.moa.backend.googlesheet.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 시트 탭 하나의 내용. 첫 행을 헤더로 본다.
 * 행의 칸 수가 헤더보다 적으면 빈 문자열로 채운다(시트 끝의 빈 칸).
 */
public record SheetTable(List<String> headers, List<List<String>> rows) {

    public SheetTable {
        headers = headers == null ? List.of() : List.copyOf(headers);
        int width = headers.size();
        List<List<String>> normalized = new ArrayList<>();
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i) == null ? List.of() : rows.get(i);
                if (row.size() > width) {
                    throw new IllegalArgumentException(
                            "%d번째 행의 칸 수(%d)가 헤더 수(%d)보다 많습니다.".formatted(i + 1, row.size(), width));
                }
                List<String> padded = new ArrayList<>(width);
                for (int c = 0; c < width; c++) {
                    String value = c < row.size() ? row.get(c) : null;
                    padded.add(value == null ? "" : value);
                }
                normalized.add(List.copyOf(padded));
            }
        }
        rows = List.copyOf(normalized);
    }

    /**
     * 각 행을 "헤더 → 값" 맵으로 바꾼다. 헤더 순서를 유지한다.
     * 빈 헤더는 "column_N", 중복 헤더는 "이름_2", "이름_3"으로 바꿔 값이 덮어써지지 않게 한다.
     */
    public List<Map<String, String>> toRecords() {
        List<String> keys = uniqueKeys();
        return rows.stream().map(row -> {
            Map<String, String> record = new LinkedHashMap<>();
            for (int i = 0; i < keys.size(); i++) {
                record.put(keys.get(i), row.get(i));
            }
            return record;
        }).toList();
    }

    private List<String> uniqueKeys() {
        List<String> keys = new ArrayList<>(headers.size());
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String base = headers.get(i) == null || headers.get(i).isBlank() ? "column_" + (i + 1) : headers.get(i);
            int count = seen.merge(base, 1, Integer::sum);
            keys.add(count == 1 ? base : base + "_" + count);
        }
        return keys;
    }
}
