package com.moa.backend.club.dto;

import com.moa.backend.club.entity.Club;
import io.swagger.v3.oas.annotations.media.Schema;

/** 웹훅 URL은 토큰이 들어 있으므로 담지 않고 연결 여부만 내려준다. */
@Schema(description = "동아리 정보")
public record ClubResponse(
		@Schema(description = "동아리 ID", example = "1")
		Long id,
		@Schema(description = "동아리 이름", example = "모아")
		String name,
		@Schema(description = "동아리 소개", example = "IT 연합 동아리", nullable = true)
		String description,
		@Schema(description = "디스코드 웹훅 연결 여부", example = "true")
		boolean discordConnected
) {

	public static ClubResponse from(Club club) {
		return new ClubResponse(club.getId(), club.getName(), club.getDescription(), club.isDiscordConnected());
	}
}
