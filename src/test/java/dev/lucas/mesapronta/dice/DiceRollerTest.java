package dev.lucas.mesapronta.dice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.lucas.mesapronta.dice.DiceRoller.RollResult;

class DiceRollerTest {

	@Test
	void rollsWithFixedSeedAndAddsModifier() {
		Random expected = new Random(42);
		int first = expected.nextInt(1, 7);
		int second = expected.nextInt(1, 7);

		RollResult result = new DiceRoller(new Random(42)).roll("2d6+3");

		assertThat(result.rolls()).containsExactly(first, second);
		assertThat(result.modifier()).isEqualTo(3);
		assertThat(result.total()).isEqualTo(first + second + 3);
		assertThat(result.describe()).isEqualTo("[" + first + ", " + second + "] + 3 = **" + result.total() + "**");
	}

	@Test
	void missingCountMeansOneDie() {
		RollResult result = new DiceRoller(new Random(1)).roll("d20");

		assertThat(result.rolls()).hasSize(1).allSatisfy(r -> assertThat(r).isBetween(1, 20));
		assertThat(result.modifier()).isZero();
	}

	@Test
	void negativeModifierAndLenientFormatting() {
		RollResult result = new DiceRoller(new Random(7)).roll(" 1D4 - 2 ");

		assertThat(result.modifier()).isEqualTo(-2);
		assertThat(result.total()).isEqualTo(result.rolls().getFirst() - 2);
		assertThat(result.describe()).contains(" - 2 = ");
	}

	@Test
	void acceptsLimits() {
		DiceRoller roller = new DiceRoller(new Random(3));

		assertThat(roller.roll("100d1000").rolls()).hasSize(100).allSatisfy(r -> assertThat(r).isBetween(1, 1000));
		assertThat(roller.roll("1d2").rolls()).allSatisfy(r -> assertThat(r).isIn(List.of(1, 2)));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "abc", "2d", "d", "0d6", "101d6", "2d1", "2d1001", "2d6+", "2d6*2", "1000d6", "2d6+3+1" })
	void rejectsInvalidExpressions(String expression) {
		DiceRoller roller = new DiceRoller(new Random(0));

		assertThatThrownBy(() -> roller.roll(expression)).isInstanceOf(IllegalArgumentException.class);
	}

}
