package com.moa.backend.googleform.adapter.in.mcp;

import com.moa.backend.googleform.application.port.in.CreateGoogleFormUseCase;
import com.moa.backend.googleform.domain.CreatedForm;
import com.moa.backend.googleform.domain.FormDraft;
import com.moa.backend.googleform.domain.FormQuestion;
import com.moa.backend.googleform.domain.QuestionType;
import com.moa.backend.mcp.McpToolSet;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Google 폼 생성 기능을 MCP 도구로 노출하는 인바운드 어댑터. */
@Component
public class GoogleFormMcpTools implements McpToolSet {

    private final CreateGoogleFormUseCase createGoogleFormUseCase;

    public GoogleFormMcpTools(CreateGoogleFormUseCase createGoogleFormUseCase) {
        this.createGoogleFormUseCase = createGoogleFormUseCase;
    }

    @Tool(name = "create_google_form",
            description = "연결된 Google 계정으로 Google 폼(설문지)을 만들고 응답을 받을 수 있게 게시한다. "
                    + "응답 링크(responderUrl)와 편집 링크(editUrl)를 돌려준다.")
    public CreatedForm createGoogleForm(
            @ToolParam(description = "폼 제목") String title,
            @ToolParam(required = false, description = "폼 설명 (선택)") String description,
            @ToolParam(description = "질문 목록. 순서대로 폼에 추가된다.") List<QuestionInput> questions) {
        List<FormQuestion> formQuestions = questions == null ? List.of()
                : questions.stream().map(QuestionInput::toDomain).toList();
        return createGoogleFormUseCase.create(new FormDraft(title, description, formQuestions));
    }

    public record QuestionInput(
            @ToolParam(description = "질문 유형: SHORT_ANSWER(단답형), PARAGRAPH(장문형), MULTIPLE_CHOICE(객관식), "
                    + "CHECKBOX(체크박스), DROPDOWN(드롭다운), LINEAR_SCALE(선형 배율), DATE(날짜), TIME(시간)")
            QuestionType type,
            @ToolParam(description = "질문 제목") String title,
            @ToolParam(required = false, description = "질문 아래 표시할 설명") String description,
            @ToolParam(required = false, description = "필수 응답 여부 (기본 false)") Boolean required,
            @ToolParam(required = false, description = "보기 목록. MULTIPLE_CHOICE, CHECKBOX, DROPDOWN일 때 필수, 중복 불가")
            List<String> options,
            @ToolParam(required = false, description = "LINEAR_SCALE 최솟값: 0 또는 1 (기본 1)") Integer scaleLow,
            @ToolParam(required = false, description = "LINEAR_SCALE 최댓값: 2~10 (기본 5)") Integer scaleHigh,
            @ToolParam(required = false, description = "LINEAR_SCALE 최솟값 쪽 라벨 (예: 전혀 아니다)") String scaleLowLabel,
            @ToolParam(required = false, description = "LINEAR_SCALE 최댓값 쪽 라벨 (예: 매우 그렇다)") String scaleHighLabel) {

        FormQuestion toDomain() {
            return new FormQuestion(type, title, description, Boolean.TRUE.equals(required), options,
                    scaleLow, scaleHigh, scaleLowLabel, scaleHighLabel);
        }
    }
}
