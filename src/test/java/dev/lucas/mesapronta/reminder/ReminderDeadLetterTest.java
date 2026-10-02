package dev.lucas.mesapronta.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import dev.lucas.mesapronta.TestcontainersConfiguration;

/** Exercises the real listener retry settings from application.yml against a RabbitMQ container. */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.retry.initial-interval=10ms")
@Import({ TestcontainersConfiguration.class, ReminderDeadLetterTest.AlwaysFailingListener.class })
class ReminderDeadLetterTest {

	@Autowired
	RabbitTemplate rabbit;

	@Autowired
	AlwaysFailingListener listener;

	@Test
	void failingReminderIsRetriedThreeTimesThenDeadLettered() {
		ReminderMessage message = new ReminderMessage(42, ReminderType.H1);

		rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, message);

		Object deadLettered = rabbit.receiveAndConvert(RabbitConfig.DLQ, 10_000);
		assertThat(deadLettered).isEqualTo(message);
		assertThat(listener.attempts).hasValue(3);
	}

	// Stands in for ReminderListener, which only exists when a Discord token is set.
	@TestConfiguration(proxyBeanMethods = false)
	static class AlwaysFailingListener {

		final AtomicInteger attempts = new AtomicInteger();

		@RabbitListener(queues = RabbitConfig.QUEUE)
		void onReminder(ReminderMessage message) {
			attempts.incrementAndGet();
			throw new IllegalStateException("Discord is down");
		}

	}

}
