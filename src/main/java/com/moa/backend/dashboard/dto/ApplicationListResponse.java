package com.moa.backend.dashboard.dto;

import com.moa.backend.dashboard.domain.ApplicationFilter;
import java.util.List;

/**
 * 신청자 응답 목록(F10).
 *
 * @param filter 적용한 필터
 * @param totalCount 필터를 적용한 행 수 (ALL은 취소 행 포함)
 * @param applicantCount 필터와 무관한 현재 신청 인원(취소 제외). 화면 제목의 신청자 수에 쓴다.
 * @param applications 신청 시각 오름차순
 */
public record ApplicationListResponse(
		ApplicationFilter filter,
		int totalCount,
		int applicantCount,
		List<ApplicationRowResponse> applications
) {
}
