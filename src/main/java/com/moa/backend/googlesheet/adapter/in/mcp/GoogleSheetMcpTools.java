package com.moa.backend.googlesheet.adapter.in.mcp;

import com.moa.backend.googlesheet.application.port.in.FetchGoogleSheetUseCase;
import com.moa.backend.mcp.McpToolSet;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 공개 Google 시트 가져오기 기능을 MCP 도구로 노출하는 인바운드 어댑터. */
@Component
public class GoogleSheetMcpTools implements McpToolSet {

    private final FetchGoogleSheetUseCase fetchGoogleSheetUseCase;

    public GoogleSheetMcpTools(FetchGoogleSheetUseCase fetchGoogleSheetUseCase) {
        this.fetchGoogleSheetUseCase = fetchGoogleSheetUseCase;
    }

    @Tool(name = "fetch_google_sheet",
            description = "'링크가 있는 모든 사용자'로 공개된 Google 시트의 탭 하나를 가져온다(Google 폼 응답 시트 포함). "
                    + "첫 행을 헤더로 보고, 각 행을 헤더 이름을 키로 한 객체로 돌려준다. "
                    + "링크에 #gid=가 있으면 그 탭, 없으면 첫 번째 탭을 가져온다.")
    public FetchSheetResult fetchGoogleSheet(
            @ToolParam(description = "시트 공유 링크 (https://docs.google.com/spreadsheets/d/{ID}/edit#gid={탭ID}) 또는 시트 ID")
            String url) {
        FetchGoogleSheetUseCase.Result result = fetchGoogleSheetUseCase.fetch(url);
        return new FetchSheetResult(
                result.ref().spreadsheetId(),
                result.ref().gid(),
                result.table().headers(),
                result.table().rows().size(),
                result.table().toRecords());
    }

    public record FetchSheetResult(String spreadsheetId, String gid, List<String> headers, int rowCount,
                                   List<Map<String, String>> records) {
    }
}
