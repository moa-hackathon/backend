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

	/** 구글폼 문항(고정 컬럼 외 CSV 헤더)의 원래 순서. MySQL JSON 객체는 키 순서를 보존하지 않아 배열로 따로 둔다. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "form_questions", columnDefinition = "json")
	private List<String> formQuestions;

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
		LocalDateTime lastSyncedAt
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
	}
}
