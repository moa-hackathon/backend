package com.moa.backend.club.entity;

import com.moa.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "club")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", nullable = true, length = 500)
	private String description;

	@Column(name = "invite_code", nullable = false, length = 64, unique = true)
	private String inviteCode;

	@Column(name = "discord_webhook_url", nullable = true, length = 255)
	private String discordWebhookUrl;

	@Builder
	private Club(String name, String description, String inviteCode, String discordWebhookUrl) {
		this.name = name;
		this.description = description;
		this.inviteCode = inviteCode;
		this.discordWebhookUrl = discordWebhookUrl;
	}

	public void connectDiscordWebhook(String discordWebhookUrl) {
		this.discordWebhookUrl = discordWebhookUrl;
	}

	public boolean isDiscordConnected() {
		return discordWebhookUrl != null && !discordWebhookUrl.isBlank();
	}
}
