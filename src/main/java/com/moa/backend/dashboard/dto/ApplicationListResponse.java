package com.moa.backend.dashboard.dto;

import com.moa.backend.dashboard.domain.ApplicationFilter;
import java.util.List;

/**
 * 신청자 응답 목록(F10).
 *
 * @param filter 적용한 필터
 * @param totalCount 필터를 적용한 행 수
 * @param applications 신청 시각 오름차순
 */
public record ApplicationListResponse(
		ApplicationFilter filter,
		int totalCount,
		List<ApplicationRowResponse> applications
) {
}
