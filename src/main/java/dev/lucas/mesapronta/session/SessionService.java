package dev.lucas.mesapronta.session;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

	public static final int MAX_PLAYERS_LIMIT = 50;

	private final SessionRepository sessions;
	private final AttendanceRepository attendances;
	private final Clock clock;

	public SessionService(SessionRepository sessions, AttendanceRepository attendances, Clock clock) {
		this.sessions = sessions;
		this.attendances = attendances;
		this.clock = clock;
	}

	@Transactional
	public Session create(String guildId, String channelId, String title, Instant startsAt, int maxPlayers,
			String createdBy) {
		if (title == null || title.isBlank() || title.length() > 100) {
			throw new IllegalArgumentException("Título deve ter entre 1 e 100 caracteres.");
		}
		if (!startsAt.isAfter(Instant.now(clock))) {
			throw new IllegalArgumentException("A data da sessão precisa estar no futuro.");
		}
		if (maxPlayers < 1 || maxPlayers > MAX_PLAYERS_LIMIT) {
			throw new IllegalArgumentException("Vagas devem ficar entre 1 e " + MAX_PLAYERS_LIMIT + ".");
		}
		return sessions.save(new Session(guildId, channelId, title.strip(), startsAt, maxPlayers, createdBy));
	}

	/**
	 * Locks the session row so that the "count seats, then insert" check is atomic:
	 * two players racing for the last seat can never both get in.
	 */
	@Transactional
	public JoinResult join(long sessionId, String userId) {
		Optional<Session> found = sessions.findByIdForUpdate(sessionId);
		if (found.isEmpty()) {
			return JoinResult.NOT_FOUND;
		}
		if (attendances.existsBySessionIdAndUserId(sessionId, userId)) {
			return JoinResult.ALREADY_IN;
		}
		if (attendances.countBySessionId(sessionId) >= found.get().getMaxPlayers()) {
			return JoinResult.FULL;
		}
		attendances.save(new Attendance(sessionId, userId));
		return JoinResult.JOINED;
	}

	@Transactional
	public boolean leave(long sessionId, String userId) {
		return attendances.deleteBySessionIdAndUserId(sessionId, userId) > 0;
	}

	@Transactional(readOnly = true)
	public Optional<Session> find(long sessionId) {
		return sessions.findById(sessionId);
	}

	@Transactional(readOnly = true)
	public List<String> attendees(long sessionId) {
		return attendances.findUserIdsBySessionId(sessionId);
	}

}
