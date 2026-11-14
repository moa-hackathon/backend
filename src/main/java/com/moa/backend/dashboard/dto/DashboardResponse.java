package com.moa.backend.dashboard.dto;

import com.moa.backend.dashboard.domain.AnswerStat;
import com.moa.backend.dashboard.domain.Banner;
import com.moa.backend.dashboard.domain.NextAction;
import com.moa.backend.event.domain.EventProgress;
import java.util.List;

/**
 * 대시보드 요약.
 *
 * @param event 행사 정보
 * @param progress 진행 상황(F6, F7)
 * @param banner 상단 배너 문구(F6)
 * @param nextAction 다음 할 일(F11)
 * @param answerStats 조건별 응답 집계(F8)
 * @param checks 확인 필요 항목(F9)
 */
public record DashboardResponse(
		EventSummaryResponse event,
		EventProgress progress,
		Banner banner,
		NextAction nextAction,
		List<AnswerStat> answerStats,
		CheckSummaryResponse checks
) {
}
