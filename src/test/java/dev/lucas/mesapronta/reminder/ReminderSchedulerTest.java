package dev.lucas.mesapronta.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import dev.lucas.mesapronta.session.Session;
import dev.lucas.mesapronta.session.SessionRepository;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

	private static final Instant NOW = Instant.parse("2026-10-01T20:00:00Z");

	@Mock
	SessionRepository sessions;

	@Mock
	RabbitTemplate rabbit;

	ReminderScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new ReminderScheduler(sessions, rabbit, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void sessionWithin24hGetsTheDayBeforeReminder() {
		Session session = session(1L, NOW.plus(Duration.ofHours(20)));
		when(sessions.lockDueReminders(any(), any(), any())).thenReturn(List.of(session));

		scheduler.enqueueDueReminders();

		verify(rabbit).convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
				new ReminderMessage(1L, ReminderType.H24));
		assertThat(session.isReminded24h()).isTrue();
		assertThat(session.isReminded1h()).isFalse();
	}

	@Test
	void sessionWithin1hGetsOnlyTheLastHourReminder() {
		Session session = session(2L, NOW.plus(Duration.ofMinutes(30)));
		when(sessions.lockDueReminders(any(), any(), any())).thenReturn(List.of(session));

		scheduler.enqueueDueReminders();

		verify(rabbit).convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
				new ReminderMessage(2L, ReminderType.H1));
		verify(rabbit, never()).convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
				new ReminderMessage(2L, ReminderType.H24));
		assertThat(session.isReminded24h()).isTrue();
		assertThat(session.isReminded1h()).isTrue();
	}

	@Test
	void nothingDuePublishesNothing() {
		when(sessions.lockDueReminders(any(), any(), any())).thenReturn(List.of());

		scheduler.enqueueDueReminders();

		verify(rabbit, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
	}

	private static Session session(long id, Instant startsAt) {
		Session session = new Session("guild", "channel", "Mesa", startsAt, 4, "creator");
		ReflectionTestUtils.setField(session, "id", id);
		return session;
	}

}
