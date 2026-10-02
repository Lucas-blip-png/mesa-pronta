package dev.lucas.mesapronta.dice;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

@Component
public class DiceRoller {

	public static final int MAX_DICE = 100;
	public static final int MAX_SIDES = 1000;

	private static final Pattern EXPRESSION = Pattern.compile("(\\d{0,3})d(\\d{1,4})([+-]\\d{1,4})?");

	private final RandomGenerator random;

	public DiceRoller() {
		this(new SecureRandom());
	}

	DiceRoller(RandomGenerator random) {
		this.random = random;
	}

	/** Rolls an "NdM+K" expression, e.g. "2d6+3", "d20", "4d8-1". */
	public RollResult roll(String expression) {
		String normalized = expression == null ? "" : expression.replace(" ", "").toLowerCase(Locale.ROOT);
		Matcher m = EXPRESSION.matcher(normalized);
		if (!m.matches()) {
			throw new IllegalArgumentException("Expressão inválida. Use o formato NdM+K, ex.: 2d6+3 ou d20.");
		}
		int count = m.group(1).isEmpty() ? 1 : Integer.parseInt(m.group(1));
		int sides = Integer.parseInt(m.group(2));
		int modifier = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
		if (count < 1 || count > MAX_DICE) {
			throw new IllegalArgumentException("Quantidade de dados deve ficar entre 1 e " + MAX_DICE + ".");
		}
		if (sides < 2 || sides > MAX_SIDES) {
			throw new IllegalArgumentException("Número de faces deve ficar entre 2 e " + MAX_SIDES + ".");
		}
		List<Integer> rolls = IntStream.range(0, count).map(i -> random.nextInt(1, sides + 1)).boxed().toList();
		int total = rolls.stream().mapToInt(Integer::intValue).sum() + modifier;
		return new RollResult(rolls, modifier, total);
	}

	public record RollResult(List<Integer> rolls, int modifier, int total) {

		/** "[4, 2] + 3 = **9**" (Discord markdown). */
		public String describe() {
			String mod = modifier == 0 ? "" : (modifier > 0 ? " + " : " - ") + Math.abs(modifier);
			return rolls + mod + " = **" + total + "**";
		}

	}

}
