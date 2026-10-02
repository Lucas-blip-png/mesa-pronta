package dev.lucas.mesapronta.reminder;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * reminders exchange -> reminders queue. Messages that still fail after the
 * listener retries are rejected and dead-lettered to reminders.dlq.
 */
@Configuration
public class RabbitConfig {

	public static final String EXCHANGE = "reminders";
	public static final String QUEUE = "reminders";
	public static final String ROUTING_KEY = "reminders";
	public static final String DLX = "reminders.dlx";
	public static final String DLQ = "reminders.dlq";

	@Bean
	DirectExchange remindersExchange() {
		return new DirectExchange(EXCHANGE);
	}

	@Bean
	Queue remindersQueue() {
		return QueueBuilder.durable(QUEUE).deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
	}

	@Bean
	Binding remindersBinding() {
		return BindingBuilder.bind(remindersQueue()).to(remindersExchange()).with(ROUTING_KEY);
	}

	@Bean
	DirectExchange deadLetterExchange() {
		return new DirectExchange(DLX);
	}

	@Bean
	Queue deadLetterQueue() {
		return QueueBuilder.durable(DLQ).build();
	}

	@Bean
	Binding deadLetterBinding() {
		return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
	}

	@Bean
	MessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter("dev.lucas.mesapronta.reminder");
	}

}
