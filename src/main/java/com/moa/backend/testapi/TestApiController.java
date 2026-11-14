package com.moa.backend.testapi;

import com.moa.backend.ai.application.port.in.GenerateTextUseCase;
import com.moa.backend.ai.domain.AiCompletion;
import com.moa.backend.ai.domain.AiProvider;
import com.moa.backend.csv.application.port.in.ReadCsvUseCase;
import com.moa.backend.csv.application.port.in.WriteCsvUseCase;
import com.moa.backend.csv.domain.CsvTable;
import com.moa.backend.discord.application.port.in.SendDiscordMessageUseCase;
import com.moa.backend.discord.domain.DiscordSendResult;
import com.moa.backend.googleauth.domain.GoogleReauthorizationRequiredException;
import com.moa.backend.googleform.application.port.in.CreateGoogleFormUseCase;
import com.moa.backend.googleform.domain.CreatedForm;
import com.moa.backend.googleform.domain.FormDraft;
import com.moa.backend.googleform.domain.FormQuestion;
import com.moa.backend.googleform.domain.QuestionType;
import com.moa.backend.googlesheet.application.port.in.FetchGoogleSheetUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 클라이언트 없이 각 기능을 HTTP로 바로 호출해 보는 테스트용 컨트롤러.
 * MCP 도구와 같은 유스케이스(in-port)를 호출한다. 운영에서는 moa.test-api.enabled=false로 끈다.
 */
@Tag(name = "Test API", description = "MCP 도구와 같은 기능을 HTTP로 호출 (운영에서는 TEST_API_ENABLED=false)")
@RestController
@RequestMapping("/test")
@ConditionalOnProperty(name = "moa.test-api.enabled", havingValue = "true")
public class TestApiController {

    private final ReadCsvUseCase readCsvUseCase;
    private final WriteCsvUseCase writeCsvUseCase;
    private final SendDiscordMessageUseCase sendDiscordMessageUseCase;
    private final CreateGoogleFormUseCase createGoogleFormUseCase;
    private final GenerateTextUseCase generateTextUseCase;
    private final FetchGoogleSheetUseCase fetchGoogleSheetUseCase;

    public TestApiController(ReadCsvUseCase readCsvUseCase,
                             WriteCsvUseCase writeCsvUseCase,
                             SendDiscordMessageUseCase sendDiscordMessageUseCase,
                             CreateGoogleFormUseCase createGoogleFormUseCase,
                             GenerateTextUseCase generateTextUseCase,
                             FetchGoogleSheetUseCase fetchGoogleSheetUseCase) {
        this.readCsvUseCase = readCsvUseCase;
        this.writeCsvUseCase = writeCsvUseCase;
        this.sendDiscordMessageUseCase = sendDiscordMessageUseCase;
        this.createGoogleFormUseCase = createGoogleFormUseCase;
        this.generateTextUseCase = generateTextUseCase;
        this.fetchGoogleSheetUseCase = fetchGoogleSheetUseCase;
    }

    // ---------- CSV ----------

    @Operation(summary = "CSV 읽기", description = "CSV 저장 디렉터리 기준 상대 경로의 파일을 읽는다.")
    @GetMapping("/csv")
    public ReadCsvResponse readCsv(
            @Parameter(description = "예: test/members.csv", example = "test/members.csv") @RequestParam String path) {
        CsvTable table = readCsvUseCase.read(path);
        return new ReadCsvResponse(path, table.headers(), table.rows().size(), table.toRecords());
    }

    @Operation(summary = "CSV 쓰기", description = "헤더와 행을 CSV 파일로 저장한다. overwrite=false면 기존 파일이 있을 때 실패.")
    @PostMapping("/csv")
    public WriteCsvResponse writeCsv(@RequestBody WriteCsvRequest request) {
        int written = writeCsvUseCase.write(request.path(), new CsvTable(request.headers(), request.rows()),
                Boolean.TRUE.equals(request.overwrite()));
        return new WriteCsvResponse(request.path(), written);
    }

    // ---------- Discord ----------

    @Operation(summary = "Discord 메시지 보내기", description = "입력한 Webhook URL의 채널로 보낸다. 2000자 초과 시 나눠 보냄(최대 10개).")
    @PostMapping("/discord")
    public DiscordSendResult sendDiscord(@RequestBody DiscordRequest request) {
        return sendDiscordMessageUseCase.send(request.webhookUrl(), request.content());
    }

    // ---------- Google 폼 ----------

    @Operation(summary = "Google 폼 만들기", description = "먼저 브라우저로 /oauth/google/authorize 에서 계정을 연결해야 한다.")
    @PostMapping("/google-form")
    public CreatedForm createGoogleForm(@RequestBody GoogleFormRequest request) {
        List<FormQuestion> questions = request.questions() == null ? List.of()
                : request.questions().stream().map(QuestionRequest::toDomain).toList();
        return createGoogleFormUseCase.create(new FormDraft(request.title(), request.description(), questions));
    }

    // ---------- Google 시트 ----------

    @Operation(summary = "공개 Google 시트 가져오기",
            description = "'링크가 있는 모든 사용자' 공개 시트를 키 없이 가져온다. 링크에 #gid= 가 있으면 그 탭, 없으면 첫 탭.")
    @GetMapping("/google-sheet")
    public SheetResponse fetchGoogleSheet(
            @Parameter(description = "시트 공유 링크 또는 시트 ID",
                    example = "https://docs.google.com/spreadsheets/d/{ID}/edit#gid=0") @RequestParam String url) {
        FetchGoogleSheetUseCase.Result result = fetchGoogleSheetUseCase.fetch(url);
        return new SheetResponse(result.ref().spreadsheetId(), result.ref().gid(), result.table().headers(),
                result.table().rows().size(), result.table().toRecords());
    }

    // ---------- AI (Upstage / Gemini) ----------

    @Operation(summary = "AI 텍스트 생성", description = "provider(UPSTAGE / GEMINI)를 골라 프롬프트를 보내고 답변을 받는다. 비우면 AI_PROVIDER 기본값. UPSTAGE_API_KEY 또는 GEMINI_API_KEY 필요.")
    @PostMapping("/ai")
    public AiCompletion generateText(@RequestBody AiRequest request) {
        return generateTextUseCase.generate(request.provider(), request.systemPrompt(), request.prompt());
    }

    // ---------- 오류 응답 ----------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(GoogleReauthorizationRequiredException.class)
    public ResponseEntity<Map<String, String>> googleNotConnected(GoogleReauthorizationRequiredException e) {
        return error(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler({IllegalStateException.class, UncheckedIOException.class})
    public ResponseEntity<Map<String, String>> failed(RuntimeException e) {
        return error(HttpStatus.BAD_GATEWAY, e);
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, RuntimeException e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    // ---------- 요청/응답 DTO ----------

    public record ReadCsvResponse(String path, List<String> headers, int rowCount, List<Map<String, String>> records) {
    }

    public record WriteCsvRequest(String path, List<String> headers, List<List<String>> rows, Boolean overwrite) {
    }

    public record WriteCsvResponse(String path, int writtenRows) {
    }

    public record DiscordRequest(String webhookUrl, String content) {
    }

    public record SheetResponse(String spreadsheetId, String gid, List<String> headers, int rowCount,
                                List<Map<String, String>> records) {
    }

    public record AiRequest(AiProvider provider, String systemPrompt, String prompt) {
    }

    public record GoogleFormRequest(String title, String description, List<QuestionRequest> questions) {
    }

    public record QuestionRequest(QuestionType type, String title, String description, Boolean required,
                                  List<String> options, Integer scaleLow, Integer scaleHigh,
                                  String scaleLowLabel, String scaleHighLabel) {

        FormQuestion toDomain() {
            return new FormQuestion(type, title, description, Boolean.TRUE.equals(required), options,
                    scaleLow, scaleHigh, scaleLowLabel, scaleHighLabel);
        }
    }
}
