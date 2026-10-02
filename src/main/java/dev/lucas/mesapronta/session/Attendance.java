package dev.lucas.mesapronta.session;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance")
public class Attendance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long sessionId;

	private String userId;

	@Column(insertable = false, updatable = false)
	private Instant createdAt;

	protected Attendance() {
	}

	public Attendance(Long sessionId, String userId) {
		this.sessionId = sessionId;
		this.userId = userId;
	}

	public Long getId() { return id; }
	public Long getSessionId() { return sessionId; }
	public String getUserId() { return userId; }
	public Instant getCreatedAt() { return createdAt; }

}
