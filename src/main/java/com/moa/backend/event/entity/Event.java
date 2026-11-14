package com.moa.backend.event.entity;

import com.moa.backend.club.entity.Club;
import com.moa.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
	name = "event",
	indexes = @Index(name = "idx_event_club_id_event_at", columnList = "club_id, event_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "club_id", nullable = false)
	private Club club;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "location", nullable = false, length = 255)
	private String location;

	@Column(name = "event_at", nullable = false)
	private LocalDateTime eventAt;

	@Column(name = "capacity", nullable = false)
	private int capacity;

	@Column(name = "min_capacity", nullable = false)
	private int minCapacity;

	@Column(name = "apply_deadline", nullable = false)
	private LocalDateTime applyDeadline;

	@Column(name = "share_code", nullable = false, unique = true, length = 32)
	private String shareCode;

	@Column(name = "last_synced_at")
	private LocalDateTime lastSyncedAt;

	/** 공지 본문. 신청 링크 자리는 {@code [구글폼 링크]}로 남겨 두고 조회할 때 바꾼다. */
	@Column(name = "notice_text", columnDefinition = "TEXT")
	private String noticeText;

	/** 신청받을 때 추가로 물어볼 질문 (예: 뒤풀이 참석 여부). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "extra_questions", columnDefinition = "json")
	private List<String> extraQuestions;

	@Column(name = "form_url", length = 500)
	private String formUrl;

	@Column(name = "notice_sent_at")
	private LocalDateTime noticeSentAt;

	@Builder
	private Event(
		Club club,
		String title,
		String description,
		String location,
		LocalDateTime eventAt,
		int capacity,
		int minCapacity,
		LocalDateTime applyDeadline,
		String shareCode,
		LocalDateTime lastSyncedAt,
		String noticeText,
		List<String> extraQuestions,
		String formUrl
	) {
		this.club = club;
		this.title = title;
		this.description = description;
		this.location = location;
		this.eventAt = eventAt;
		this.capacity = capacity;
		this.minCapacity = minCapacity;
		this.applyDeadline = applyDeadline;
		this.shareCode = shareCode;
		this.lastSyncedAt = lastSyncedAt;
		this.noticeText = noticeText;
		this.extraQuestions = extraQuestions;
		this.formUrl = formUrl;
	}

	public void updateNotice(String noticeText) {
		this.noticeText = noticeText;
	}

	public void connectForm(String formUrl) {
		this.formUrl = formUrl;
	}

	public void markNoticeSent(LocalDateTime sentAt) {
		this.noticeSentAt = sentAt;
	}
}
