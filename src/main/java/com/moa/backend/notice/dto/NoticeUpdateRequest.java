package com.moa.backend.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeUpdateRequest(
		@NotBlank @Size(max = 5000) String noticeText
) {
}
