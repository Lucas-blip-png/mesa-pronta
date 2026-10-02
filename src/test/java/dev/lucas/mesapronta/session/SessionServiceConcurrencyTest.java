package dev.lucas.mesapronta.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SessionService.class)
// Each service call must commit for real, otherwise the row lock is never contended.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SessionServiceConcurrencyTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	SessionService service;

	@Autowired
	SessionRepository sessions;

	@Autowired
	AttendanceRepository attendances;

	@AfterEach
	void cleanUp() {
		attendances.deleteAll();
		sessions.deleteAll();
	}

	@Test
	void onlyOnePlayerGetsTheLastSeat() throws Exception {
		long id = newSession(1);
		int players = 20;
		CountDownLatch start = new CountDownLatch(1);
		List<Future<JoinResult>> results = new ArrayList<>();

		try (ExecutorService pool = Executors.newFixedThreadPool(players)) {
			for (int i = 0; i < players; i++) {
				String userId = "user-" + i;
				results.add(pool.submit(() -> {
					start.await();
					return service.join(id, userId);
				}));
			}
			start.countDown();

			List<JoinResult> outcomes = new ArrayList<>();
			for (Future<JoinResult> result : results) {
				outcomes.add(result.get());
			}
			assertThat(outcomes).containsOnlyOnce(JoinResult.JOINED);
			assertThat(outcomes).filteredOn(r -> r == JoinResult.FULL).hasSize(players - 1);
		}
		assertThat(attendances.countBySessionId(id)).isEqualTo(1);
	}

	@Test
	void joiningTwiceIsRejected() {
		long id = newSession(5);

		assertThat(service.join(id, "user-1")).isEqualTo(JoinResult.JOINED);
		assertThat(service.join(id, "user-1")).isEqualTo(JoinResult.ALREADY_IN);
		assertThat(attendances.countBySessionId(id)).isEqualTo(1);
	}

	@Test
	void joiningUnknownSessionReturnsNotFound() {
		assertThat(service.join(999_999L, "user-1")).isEqualTo(JoinResult.NOT_FOUND);
	}

	@Test
	void leavingFreesTheSeat() {
		long id = newSession(1);
		service.join(id, "user-1");

		assertThat(service.join(id, "user-2")).isEqualTo(JoinResult.FULL);
		assertThat(service.leave(id, "user-1")).isTrue();
		assertThat(service.join(id, "user-2")).isEqualTo(JoinResult.JOINED);
		assertThat(service.attendees(id)).containsExactly("user-2");
	}

	private long newSession(int maxPlayers) {
		Instant startsAt = Instant.now().plus(Duration.ofDays(2));
		return service.create("guild", "channel", "One-shot", startsAt, maxPlayers, "gm").getId();
	}

}
