package dev.lucas.mesapronta.discord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import dev.lucas.mesapronta.dice.DiceRoller;
import dev.lucas.mesapronta.session.Session;
import dev.lucas.mesapronta.session.SessionService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.TimeFormat;

@Component
@ConditionalOnExpression(DiscordConfig.ENABLED)
class SlashCommandListener extends ListenerAdapter {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
		.withResolverStyle(ResolverStyle.STRICT);

	private final SessionService sessions;
	private final DiceRoller dice;
	private final ZoneId zone;

	SlashCommandListener(SessionService sessions, DiceRoller dice, @Value("${app.zone}") ZoneId zone) {
		this.sessions = sessions;
		this.dice = dice;
		this.zone = zone;
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		switch (event.getName()) {
			case "agendar" -> schedule(event);
			case "rolar" -> roll(event);
			case "mesas" -> list(event);
			default -> {
			}
		}
	}

	private void schedule(SlashCommandInteractionEvent event) {
		if (!event.isFromGuild()) {
			event.reply("Esse comando só funciona dentro de um servidor.").setEphemeral(true).queue();
			return;
		}
		try {
			Instant startsAt = parseDate(event.getOption("data", OptionMapping::getAsString), zone);
			Session session = sessions.create(event.getGuild().getId(), event.getChannelId(),
					event.getOption("titulo", OptionMapping::getAsString), startsAt,
					event.getOption("vagas", OptionMapping::getAsInt), event.getUser().getId());
			event.replyEmbeds(sessionEmbed(session, 0))
				.addComponents(ActionRow.of(Button.success("join:" + session.getId(), "Confirmar presença"),
						Button.danger("leave:" + session.getId(), "Sair")))
				.queue();
		}
		catch (IllegalArgumentException e) {
			event.reply(e.getMessage()).setEphemeral(true).queue();
		}
	}

	private void list(SlashCommandInteractionEvent event) {
		if (!event.isFromGuild()) {
			event.reply("Esse comando só funciona dentro de um servidor.").setEphemeral(true).queue();
			return;
		}
		List<Session> upcoming = sessions.upcoming(event.getGuild().getId());
		if (upcoming.isEmpty()) {
			event.reply("Nenhuma mesa agendada. Use /agendar para criar uma.").setEphemeral(true).queue();
			return;
		}
		String lines = upcoming.stream()
			.map(s -> "**#%d %s** — %s · %d/%d vagas".formatted(s.getId(), s.getTitle(),
					TimeFormat.DATE_TIME_SHORT.format(s.getStartsAt().toEpochMilli()), sessions.attendees(s.getId()).size(),
					s.getMaxPlayers()))
			.collect(Collectors.joining("\n"));
		event.replyEmbeds(new EmbedBuilder().setTitle("📅 Próximas mesas").setDescription(lines).build())
			.setEphemeral(true)
			.queue();
	}

	private void roll(SlashCommandInteractionEvent event) {
		String expression = event.getOption("expressao", OptionMapping::getAsString);
		try {
			// Safe to echo: roll() only accepts digits, "d", "+", "-" and spaces.
			event.reply("🎲 " + expression.strip() + " → " + dice.roll(expression).describe()).queue();
		}
		catch (IllegalArgumentException e) {
			event.reply(e.getMessage()).setEphemeral(true).queue();
		}
	}

	static Instant parseDate(String text, ZoneId zone) {
		try {
			return LocalDateTime.parse(text.strip(), DATE_FORMAT).atZone(zone).toInstant();
		}
		catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Data inválida. Use dd/MM/aaaa HH:mm, ex.: 25/12/2026 19:30.");
		}
	}

	static MessageEmbed sessionEmbed(Session session, long confirmed) {
		long startsAt = session.getStartsAt().toEpochMilli();
		return new EmbedBuilder().setTitle("🎲 " + session.getTitle())
			.addField("Quando", TimeFormat.DATE_TIME_LONG.format(startsAt) + " (" + TimeFormat.RELATIVE.format(startsAt) + ")",
					false)
			.addField("Vagas", confirmed + "/" + session.getMaxPlayers(), true)
			.setFooter("Mesa #" + session.getId())
			.build();
	}

}
