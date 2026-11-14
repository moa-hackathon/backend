package com.moa.backend.googlesheet.adapter.out.export;

import com.moa.backend.googlesheet.application.port.out.GoogleSheetPort;
import com.moa.backend.googlesheet.domain.GoogleSheetRef;
import com.moa.backend.googlesheet.domain.SheetTable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * "링크가 있는 모든 사용자" 공개 시트를 키 없이 CSV로 내려받는 어댑터.
 * GET https://docs.google.com/spreadsheets/d/{ID}/export?format=csv&gid={탭ID}
 * 공개가 아니면 Google이 로그인 페이지(HTML)를 돌려주므로 Content-Type으로 구분한다.
 */
@Component
public class PublicSheetCsvExportAdapter implements GoogleSheetPort {

    static final String BASE_URL = "https://docs.google.com/spreadsheets/d";
    /** 너무 큰 시트로 메모리가 터지지 않게 막는 상한 */
    static final int MAX_BYTES = 10 * 1024 * 1024;

    private final RestClient restClient;

    public PublicSheetCsvExportAdapter(RestClient.Builder restClientBuilder) {
        // export 주소는 googleusercontent.com 으로 리다이렉트되므로 리다이렉트를 따라간다(https→http 제외).
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = restClientBuilder.clone().requestFactory(requestFactory).build();
    }

    @Override
    public SheetTable fetch(GoogleSheetRef ref) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(BASE_URL)
                .pathSegment(ref.spreadsheetId(), "export")
                .queryParam("format", "csv");
        if (ref.gid() != null) {
            builder.queryParam("gid", ref.gid());
        }
        URI uri = builder.build().encode().toUri();

        String csv;
        try {
            csv = restClient.get()
                    .uri(uri)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        MediaType type = response.getHeaders().getContentType();
                        boolean isCsv = type != null && type.getSubtype().toLowerCase().contains("csv");
                        if (status == 404) {
                            throw new IllegalArgumentException("Google 시트를 찾을 수 없습니다. 링크의 시트 ID를 확인하세요.");
                        }
                        if (status == 400 && ref.gid() != null) {
                            throw new IllegalArgumentException("시트에 해당 탭(gid=%s)이 없습니다.".formatted(ref.gid()));
                        }
                        if ((status == 200 && !isCsv) || status == 401 || status == 403) {
                            throw new IllegalArgumentException(
                                    "시트가 공개되어 있지 않습니다. 공유 설정에서 '링크가 있는 모든 사용자'를 '뷰어'로 바꿔주세요.");
                        }
                        if (status != 200) {
                            throw new IllegalStateException("Google 시트 내려받기 실패 (HTTP %d)".formatted(status));
                        }
                        return readLimited(response.getBody());
                    }, true);
        } catch (RestClientException e) {
            throw new IllegalStateException("Google 시트 요청에 실패했습니다(네트워크/타임아웃): " + e.getClass().getSimpleName(), e);
        }
        return parse(csv);
    }

    private static String readLimited(InputStream body) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = body.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BYTES) {
                throw new IllegalArgumentException("시트가 너무 큽니다(최대 %dMB).".formatted(MAX_BYTES / 1024 / 1024));
            }
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static SheetTable parse(String csv) {
        try (CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(csv))) {
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                return new SheetTable(List.of(), List.of());
            }
            List<String> headers = records.get(0).toList();
            List<List<String>> rows = new ArrayList<>(records.size() - 1);
            for (CSVRecord record : records.subList(1, records.size())) {
                rows.add(record.toList());
            }
            return new SheetTable(headers, rows);
        } catch (IOException e) {
            throw new UncheckedIOException("Google 시트 CSV를 읽는 중 오류가 발생했습니다.", e);
        }
    }
}
