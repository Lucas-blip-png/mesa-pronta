package dev.lucas.mesapronta.discord;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import dev.lucas.mesapronta.session.JoinResult;
import dev.lucas.mesapronta.session.Session;
import dev.lucas.mesapronta.session.SessionService;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
@ConditionalOnExpression(DiscordConfig.ENABLED)
class ButtonListener extends ListenerAdapter {

	private final SessionService sessions;

	ButtonListener(SessionService sessions) {
		this.sessions = sessions;
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String[] parts = event.getComponentId().split(":", 2);
		if (parts.length != 2) {
			return;
		}
		long sessionId;
		try {
			sessionId = Long.parseLong(parts[1]);
		}
		catch (NumberFormatException e) {
			return;
		}
		String userId = event.getUser().getId();
		switch (parts[0]) {
			case "join" -> {
				JoinResult result = sessions.join(sessionId, userId);
				if (result == JoinResult.JOINED) {
					updateEmbedAndConfirm(event, sessionId, message(result));
				}
				else {
					event.reply(message(result)).setEphemeral(true).queue();
				}
			}
			case "leave" -> {
				if (sessions.leave(sessionId, userId)) {
					updateEmbedAndConfirm(event, sessionId, "Você saiu da mesa.");
				}
				else {
					event.reply("Você não estava nesta mesa.").setEphemeral(true).queue();
				}
			}
			default -> {
			}
		}
	}

	private void updateEmbedAndConfirm(ButtonInteractionEvent event, long sessionId, String text) {
		Optional<Session> session = sessions.find(sessionId);
		if (session.isEmpty()) {
			event.reply(text).setEphemeral(true).queue();
			return;
		}
		event.editMessageEmbeds(SlashCommandListener.sessionEmbed(session.get(), sessions.attendees(sessionId).size()))
			.queue(hook -> hook.sendMessage(text).setEphemeral(true).queue());
	}

	static String message(JoinResult result) {
		return switch (result) {
			case JOINED -> "Presença confirmada! ✅";
			case FULL -> "Essa mesa já está lotada. 😕";
			case ALREADY_IN -> "Você já está nesta mesa.";
			case NOT_FOUND -> "Essa mesa não existe mais.";
		};
	}

}
