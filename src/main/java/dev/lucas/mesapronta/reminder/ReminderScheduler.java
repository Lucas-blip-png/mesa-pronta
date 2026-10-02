package dev.lucas.mesapronta.reminder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.lucas.mesapronta.session.Session;
import dev.lucas.mesapronta.session.SessionRepository;

@Component
public class ReminderScheduler {

	private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

	private final SessionRepository sessions;
	private final RabbitTemplate rabbit;
	private final Clock clock;

	public ReminderScheduler(SessionRepository sessions, RabbitTemplate rabbit, Clock clock) {
		this.sessions = sessions;
		this.rabbit = rabbit;
		this.clock = clock;
	}

	// ponytail: publish + flag update is not atomic; if the commit fails after the publish,
	// the reminder is sent again on the next poll. Upgrade path: transactional outbox.
	@Scheduled(fixedDelayString = "${app.reminders.poll-interval}")
	@Transactional
	public void enqueueDueReminders() {
		Instant now = Instant.now(clock);
		Instant in1h = now.plus(Duration.ofHours(1));
		for (Session session : sessions.lockDueReminders(now, now.plus(Duration.ofHours(24)), in1h)) {
			if (!session.isReminded1h() && !session.getStartsAt().isAfter(in1h)) {
				// Too late for the 24h one: send only the 1h reminder.
				publish(session, ReminderType.H1);
				session.markReminded24h();
				session.markReminded1h();
			}
			else if (!session.isReminded24h()) {
				publish(session, ReminderType.H24);
				session.markReminded24h();
			}
		}
	}

	private void publish(Session session, ReminderType type) {
		rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
				new ReminderMessage(session.getId(), type));
		log.info("reminder_enqueued sessionId={} type={}", session.getId(), type);
	}

}
