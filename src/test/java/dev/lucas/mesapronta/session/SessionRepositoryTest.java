package dev.lucas.mesapronta.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SessionRepositoryTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	SessionRepository sessions;

	@Test
	void upcomingListsOnlyFutureSessionsOfTheGuildInDateOrder() {
		Instant now = Instant.parse("2026-10-01T12:00:00Z");
		save("guild", "Later", now.plus(Duration.ofDays(3)));
		save("guild", "Past", now.minus(Duration.ofHours(1)));
		save("guild", "Sooner", now.plus(Duration.ofHours(2)));
		save("other-guild", "Elsewhere", now.plus(Duration.ofHours(1)));

		assertThat(sessions.findTop10ByGuildIdAndStartsAtAfterOrderByStartsAt("guild", now))
			.extracting(Session::getTitle)
			.containsExactly("Sooner", "Later");
	}

	private void save(String guildId, String title, Instant startsAt) {
		sessions.save(new Session(guildId, "channel", title, startsAt, 4, "gm"));
	}

}
