package dev.lucas.mesapronta.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

	static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
	static final Instant TOMORROW = NOW.plus(Duration.ofDays(1));

	@Mock
	SessionRepository sessions;

	@Mock
	AttendanceRepository attendances;

	SessionService service;

	@BeforeEach
	void setUp() {
		service = new SessionService(sessions, attendances, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void createsSessionWithTrimmedTitle() {
		when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

		Session session = service.create("g", "c", "  Curse of Strahd  ", TOMORROW, 4, "gm");

		assertThat(session.getTitle()).isEqualTo("Curse of Strahd");
	}

	@Test
	void rejectsBlankTitle() {
		assertThatThrownBy(() -> service.create("g", "c", "  ", TOMORROW, 4, "gm"))
			.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(sessions);
	}

	@Test
	void rejectsSessionInThePast() {
		assertThatThrownBy(() -> service.create("g", "c", "Mesa", NOW, 4, "gm"))
			.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(sessions);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, SessionService.MAX_PLAYERS_LIMIT + 1 })
	void rejectsSeatCountOutOfRange(int maxPlayers) {
		assertThatThrownBy(() -> service.create("g", "c", "Mesa", TOMORROW, maxPlayers, "gm"))
			.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(sessions);
	}

}
