package com.moa.backend.csv.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 헤더와 데이터 행으로 구성된 CSV 표.
 * 모든 행의 컬럼 수는 헤더 수와 같아야 한다.
 */
public record CsvTable(List<String> headers, List<List<String>> rows) {

    public CsvTable {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("CSV 헤더는 비어 있을 수 없습니다.");
        }
        headers = List.copyOf(headers);
        if (rows == null) {
            rows = List.of();
        }
        List<List<String>> copied = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = Objects.requireNonNull(rows.get(i), (i + 1) + "번째 행이 null입니다.");
            if (row.size() != headers.size()) {
                throw new IllegalArgumentException(
                        "%d번째 행의 컬럼 수(%d)가 헤더 수(%d)와 다릅니다.".formatted(i + 1, row.size(), headers.size()));
            }
            // List.copyOf는 null 요소를 허용하지 않으므로 빈 문자열로 치환한다.
            copied.add(row.stream().map(value -> value == null ? "" : value).toList());
        }
        rows = List.copyOf(copied);
    }

    /** 각 행을 헤더 순서를 유지한 Map으로 변환한다. */
    public List<Map<String, String>> toRecords() {
        List<Map<String, String>> records = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            Map<String, String> record = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                record.put(headers.get(i), row.get(i));
            }
            records.add(record);
        }
        return records;
    }
}
