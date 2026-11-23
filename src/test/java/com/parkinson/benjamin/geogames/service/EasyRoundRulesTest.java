package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.LocatedPlaceName;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The rules of an Easy game: more guesses, and no continent hint for rivers and lakes. */
class EasyRoundRulesTest {

  private static final List<String> COUNTRIES =
      IntStream.range(0, 12).mapToObj(i -> "Country" + (char) ('A' + i)).toList();

  private final RoundRules rules = new RoundRules(namesOfTwelveCountries());

  private static KnownPlaceNames namesOfTwelveCountries() {
    CountryNameService countries = mock(CountryNameService.class);
    when(countries.loadCountryNames())
        .thenReturn(COUNTRIES.stream().map(n -> new LocatedPlaceName(n, List.of(), 0, 0)).toList());
    return new KnownPlaceNames(
        countries, mock(RiverLoaderService.class), mock(LakeLoaderService.class));
  }

  private static GameRoundEntity fresh(GameType type) {
    return new GameRoundEntity(0, type, "{}");
  }

  @Test
  void anEasyRoundAcceptsEightWrongGuessesAndRejectsTheNinth() {
    GameRoundEntity round = fresh(GameType.COUNTRIES_BY_COASTLINE);

    for (int i = 0; i < 8; i++) {
      assertThat(rules.addGuess(round, Difficulty.EASY, COUNTRIES.get(i), false)).isTrue();
    }

    assertThatThrownBy(() -> rules.addGuess(round, Difficulty.EASY, COUNTRIES.get(8), false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("8 wrong");
  }

  @Test
  void aNormalRoundStillStopsAtFive() {
    GameRoundEntity round = fresh(GameType.COUNTRIES_BY_COASTLINE);
    for (int i = 0; i < 5; i++) {
      rules.addGuess(round, Difficulty.NORMAL, COUNTRIES.get(i), false);
    }

    assertThatThrownBy(() -> rules.addGuess(round, Difficulty.NORMAL, COUNTRIES.get(5), false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void outOfGuessesNeedsExactlyTheEffectiveLimit() {
    GameRoundEntity round = fresh(GameType.COUNTRIES_BY_COASTLINE);

    rules.validateAnswer(round, Difficulty.EASY, AnswerState.OUT_OF_GUESSES, 0, 8, null);
    assertThatThrownBy(
            () ->
                rules.validateAnswer(
                    round, Difficulty.EASY, AnswerState.OUT_OF_GUESSES, 0, 5, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> rules.validateAnswer(round, Difficulty.EASY, AnswerState.GAVE_UP, 0, 8, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> rules.validateAnswer(round, Difficulty.EASY, AnswerState.GAVE_UP, 0, 9, null))
        .isInstanceOf(IllegalArgumentException.class);
    // Five wrong guesses is only a give-up in Easy, not the end of the round.
    rules.validateAnswer(round, Difficulty.EASY, AnswerState.GAVE_UP, 0, 5, null);
  }

  @Test
  void anEasyRiverByShapeRoundHasTwoHints() {
    GameRoundEntity round = fresh(GameType.RIVERS_BY_SHAPE);
    rules.addHint(round, Difficulty.EASY, 0, 0);
    rules.addHint(round, Difficulty.EASY, 1, 0);

    assertThatThrownBy(() -> rules.addHint(round, Difficulty.EASY, 2, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2 hints");
    assertThatThrownBy(
            () -> rules.validateAnswer(round, Difficulty.EASY, AnswerState.CORRECT, 3, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void twoHintsInAnEasyRiverRoundScoreFifty() {
    GameRoundEntity round = fresh(GameType.RIVERS_BY_SHAPE);
    round.recordAnswer(AnswerState.CORRECT, 2, 0, 1);

    assertThat(GameResults.pointsFor(round, Difficulty.EASY)).isEqualTo(50);
    assertThat(GameResults.pointsFor(round, Difficulty.NORMAL)).isEqualTo(67);
  }

  @Test
  void aTypeWithoutAContinentHintKeepsItsHintsInEasy() {
    GameRoundEntity round = fresh(GameType.RIVERS_BY_COUNTRIES);
    rules.addHint(round, Difficulty.EASY, 0, 0);
    rules.addHint(round, Difficulty.EASY, 1, 0);

    assertThatThrownBy(() -> rules.addHint(round, Difficulty.EASY, 2, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
