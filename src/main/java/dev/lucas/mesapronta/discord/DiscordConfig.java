package dev.lucas.mesapronta.discord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.lucas.mesapronta.session.SessionService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Configuration
@ConditionalOnExpression(DiscordConfig.ENABLED)
public class DiscordConfig {

	// @ConditionalOnProperty would match the empty default of DISCORD_TOKEN, so check for blank explicitly.
	public static final String ENABLED = "!'${discord.token:}'.isBlank()";

	@Bean(destroyMethod = "shutdown")
	JDA jda(@Value("${discord.token}") String token, SlashCommandListener slashCommands, ButtonListener buttons)
			throws InterruptedException {
		JDA jda = JDABuilder.createLight(token).addEventListeners(slashCommands, buttons).build().awaitReady();
		jda.updateCommands().addCommands(
				Commands.slash("agendar", "Agenda uma sessão de RPG neste canal")
					.setContexts(InteractionContextType.GUILD)
					.addOptions(
							new OptionData(OptionType.STRING, "titulo", "Nome da sessão", true).setRequiredLength(1, 100),
							new OptionData(OptionType.STRING, "data", "Data e hora (dd/MM/yyyy HH:mm)", true)
								.setRequiredLength(16, 16),
							new OptionData(OptionType.INTEGER, "vagas", "Número de vagas", true)
								.setRequiredRange(1, SessionService.MAX_PLAYERS_LIMIT)),
				Commands.slash("mesas", "Lista as próximas sessões deste servidor")
					.setContexts(InteractionContextType.GUILD),
				Commands.slash("rolar", "Rola dados, ex.: 2d6+3")
					.addOptions(new OptionData(OptionType.STRING, "expressao", "Ex.: d20, 2d6+3, 4d8-1", true)
						.setMaxLength(20)))
			.queue();
		return jda;
	}

}
