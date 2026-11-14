package com.moa.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FormLinkRequest(
		@Schema(description = "구글폼 응답자용 링크 (forms.gle 또는 docs.google.com/forms)",
				example = "https://forms.gle/AbCdEf123")
		@NotBlank
		@Size(max = 500)
		String formUrl
) {
}
