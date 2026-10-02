package dev.lucas.mesapronta.reminder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import dev.lucas.mesapronta.discord.DiscordConfig;
import dev.lucas.mesapronta.session.Session;
import dev.lucas.mesapronta.session.SessionService;

/**
 * Posts reminders to the session's channel. A Discord failure throws, so the
 * listener retry kicks in and, once exhausted, the message goes to the DLQ.
 */
@Component
@ConditionalOnExpression(DiscordConfig.ENABLED)
public class ReminderListener {

	private static final Logger log = LoggerFactory.getLogger(ReminderListener.class);

	private final JDA jda;
	private final SessionService sessionService;

	public ReminderListener(JDA jda, SessionService sessionService) {
		this.jda = jda;
		this.sessionService = sessionService;
	}

	@RabbitListener(queues = RabbitConfig.QUEUE)
	public void onReminder(ReminderMessage message) {
		Optional<Session> found = sessionService.find(message.sessionId());
		if (found.isEmpty()) {
			log.warn("reminder_skipped reason=session_not_found sessionId={}", message.sessionId());
			return;
		}
		Session session = found.get();
		TextChannel channel = jda.getTextChannelById(session.getChannelId());
		if (channel == null) {
			// Retrying cannot bring a deleted channel back.
			log.warn("reminder_skipped reason=channel_not_found sessionId={} channelId={}", session.getId(),
					session.getChannelId());
			return;
		}
		channel.sendMessage(format(session, message.type(), sessionService.attendees(session.getId()))).complete();
		log.info("reminder_sent sessionId={} type={}", session.getId(), message.type());
	}

	static String format(Session session, ReminderType type, List<String> attendees) {
		String text = "⏰ **%s** começa em %s (<t:%d:F>)".formatted(session.getTitle(), type.label(),
				session.getStartsAt().getEpochSecond());
		if (attendees.isEmpty()) {
			return text + "\nNinguém confirmou presença ainda.";
		}
		return text + "\n" + attendees.stream().map(id -> "<@" + id + ">").collect(Collectors.joining(" "));
	}

}
