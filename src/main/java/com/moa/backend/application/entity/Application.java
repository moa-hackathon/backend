package com.moa.backend.application.entity;

import com.moa.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
	name = "application",
	indexes = @Index(name = "idx_application_event_id_student_id", columnList = "event_id, student_id"),
	uniqueConstraints = @UniqueConstraint(name = "uk_application_event_id_row_hash", columnNames = {"event_id", "row_hash"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false)
	private Long eventId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "student_id", nullable = true, length = 20)
	private String studentId;

	@Column(name = "contact", nullable = true, length = 100)
	private String contact;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ApplicationStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 10)
	private ApplicationSource source;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "answers", nullable = true, columnDefinition = "json")
	private Map<String, String> answers;

	@Column(name = "row_hash", nullable = true, length = 64, columnDefinition = "CHAR(64)")
	private String rowHash;

	@Column(name = "applied_at", nullable = false)
	private LocalDateTime appliedAt;

	@Builder
	private Application(
		Long eventId,
		String name,
		String studentId,
		String contact,
		ApplicationStatus status,
		ApplicationSource source,
		Map<String, String> answers,
		String rowHash,
		LocalDateTime appliedAt
	) {
		this.eventId = eventId;
		this.name = name;
		this.studentId = studentId;
		this.contact = contact;
		this.status = status;
		this.source = source;
		this.answers = answers;
		this.rowHash = rowHash;
		this.appliedAt = appliedAt;
	}
}
