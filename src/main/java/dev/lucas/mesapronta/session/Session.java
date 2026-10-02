package dev.lucas.mesapronta.session;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "session")
public class Session {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String guildId;

	private String channelId;

	private String title;

	private Instant startsAt;

	private int maxPlayers;

	private String createdBy;

	@Column(name = "reminded_24h")
	private boolean reminded24h;

	@Column(name = "reminded_1h")
	private boolean reminded1h;

	@Column(insertable = false, updatable = false)
	private Instant createdAt;

	protected Session() {
	}

	public Session(String guildId, String channelId, String title, Instant startsAt, int maxPlayers, String createdBy) {
		this.guildId = guildId;
		this.channelId = channelId;
		this.title = title;
		this.startsAt = startsAt;
		this.maxPlayers = maxPlayers;
		this.createdBy = createdBy;
	}

	public Long getId() { return id; }
	public String getGuildId() { return guildId; }
	public String getChannelId() { return channelId; }
	public String getTitle() { return title; }
	public Instant getStartsAt() { return startsAt; }
	public int getMaxPlayers() { return maxPlayers; }
	public String getCreatedBy() { return createdBy; }
	public boolean isReminded24h() { return reminded24h; }
	public boolean isReminded1h() { return reminded1h; }
	public Instant getCreatedAt() { return createdAt; }

	public void markReminded24h() { this.reminded24h = true; }
	public void markReminded1h() { this.reminded1h = true; }

}
