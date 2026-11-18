package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.RoundGuess;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RoundRulesTest {

  private final RoundRules rules = new RoundRules(GameServiceTest.testNames());

  private static GameRoundEntity round(int index, AnswerState state) {
    GameRoundEntity entity = new GameRoundEntity(index, GameType.TRIPOINT, "{}");
    entity.setAnswerState(state);
    return entity;
  }

  private static GameRoundEntity guessRound(GameType type, AnswerState state) {
    GameRoundEntity entity = new GameRoundEntity(0, type, "{}");
    entity.setAnswerState(state);
    return entity;
  }

  // ------------------------------------------------------ wrong guesses

  /** Answers a fresh TRIPOINT round (guess limit 5) and returns it. */
  private GameRoundEntity submitToFreshRound(AnswerState state, Integer wrongGuesses) {
    GameRoundEntity round = round(0, AnswerState.UNANSWERED);
    rules.validateAnswer(round, state, 0, wrongGuesses);
    rules.recordAnswer(round, state, 0, wrongGuesses);
    return round;
  }

  private GameRoundEntity expectRejected(AnswerState state, Integer wrongGuesses, String message) {
    GameRoundEntity stored = round(0, AnswerState.UNANSWERED);
    assertThatThrownBy(() -> rules.validateAnswer(stored, state, 0, wrongGuesses))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(message);
    assertThat(stored.getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    assertThat(stored.getWrongGuesses()).isNull();
    return stored;
  }

  @Test
  void submitAnswerStoresWrongGuesses() {
    assertThat(submitToFreshRound(AnswerState.CORRECT, 3).getWrongGuesses()).isEqualTo(3);
  }

  @Test
  void aMissingWrongGuessesIsStoredAsUnknownNotZero() {
    assertThat(submitToFreshRound(AnswerState.CORRECT, null).getWrongGuesses()).isNull();
    assertThat(submitToFreshRound(AnswerState.GAVE_UP, null).getWrongGuesses()).isNull();
    assertThat(submitToFreshRound(AnswerState.OUT_OF_GUESSES, null).getWrongGuesses()).isNull();
  }

  @Test
  void correctAndGaveUpAcceptZeroUpToOneBelowTheLimit() {
    assertThat(submitToFreshRound(AnswerState.CORRECT, 0).getWrongGuesses()).isZero();
    assertThat(submitToFreshRound(AnswerState.CORRECT, 4).getWrongGuesses()).isEqualTo(4);
    assertThat(submitToFreshRound(AnswerState.GAVE_UP, 0).getWrongGuesses()).isZero();
    assertThat(submitToFreshRound(AnswerState.GAVE_UP, 4).getWrongGuesses()).isEqualTo(4);
  }

  @Test
  void outOfGuessesAcceptsExactlyTheLimit() {
    assertThat(submitToFreshRound(AnswerState.OUT_OF_GUESSES, 5).getWrongGuesses()).isEqualTo(5);
  }

  @Test
  void negativeWrongGuessesAreRejected() {
    expectRejected(AnswerState.CORRECT, -1, "-1");
  }

  @Test
  void moreWrongGuessesThanTheLimitAreRejected() {
    expectRejected(AnswerState.CORRECT, 6, "allows 5");
    expectRejected(AnswerState.OUT_OF_GUESSES, 6, "allows 5");
  }

  @Test
  void outOfGuessesWithFewerThanTheLimitIsRejected() {
    expectRejected(
        AnswerState.OUT_OF_GUESSES,
        3,
        "A round of TRIPOINT that ran out of guesses must have 5 wrong guesses, not 3");
  }

  @Test
  void correctOrGaveUpWithTheLimitIsRejected() {
    expectRejected(AnswerState.CORRECT, 5, "ran out of guesses");
    expectRejected(AnswerState.GAVE_UP, 5, "ran out of guesses");
  }

  @Test
  void sameAnswerWithSameWrongGuessesIsANoOp() {
    GameRoundEntity answered = round(0, AnswerState.CORRECT);
    answered.setWrongGuesses(2);

    assertThat(rules.recordAnswer(answered, AnswerState.CORRECT, 0, 2)).isFalse();
  }

  @Test
  void sameAnswerWithDifferentWrongGuessesIsAConflict() {
    GameRoundEntity answered = round(0, AnswerState.CORRECT);
    answered.setWrongGuesses(2);

    assertThatThrownBy(() -> rules.recordAnswer(answered, AnswerState.CORRECT, 0, 3))
        .isInstanceOf(RoundAlreadyAnsweredException.class)
        .hasMessageContaining("2 wrong guesses")
        .hasMessageContaining("3 wrong guesses");

    assertThat(answered.getWrongGuesses()).isEqualTo(2);
  }

  @Test
  void unreportedWrongGuessesOnlyMatchUnreported() {
    GameRoundEntity unreported = round(0, AnswerState.CORRECT);

    assertThat(rules.recordAnswer(unreported, AnswerState.CORRECT, 0, null)).isFalse();

    assertThatThrownBy(() -> rules.recordAnswer(unreported, AnswerState.CORRECT, 0, 0))
        .isInstanceOf(RoundAlreadyAnsweredException.class)
        .hasMessageContaining("unreported");
  }

  // ------------------------------------------------------------ addGuess

  @ParameterizedTest
  @ValueSource(strings = {"Atlantis", "", "   "})
  void anUnknownOrBlankGuessIsRejected(String guess) {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);

    assertThatThrownBy(() -> rules.addGuess(entity, guess, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(entity.getGuesses()).isEmpty();
  }

  @Test
  void aNullOrTooLongGuessIsRejected() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);

    assertThatThrownBy(() -> rules.addGuess(entity, null, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> rules.addGuess(entity, "a".repeat(256), false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void theLastAllowedWrongGuessIsAcceptedAndTheNextIsRejected() {
    GameRoundEntity entity = guessRound(GameType.LAKES_BY_SHAPE, AnswerState.UNANSWERED);
    List<String> lakes = List.of("Garda", "Como", "Maggiore", "Lugano", "Iseo", "Orta");
    int limit = GameType.LAKES_BY_SHAPE.guessLimit();
    assertThat(lakes.size()).isGreaterThan(limit);

    for (int i = 0; i < limit; i++) {
      rules.addGuess(entity, lakes.get(i), false);
    }

    assertThatThrownBy(() -> rules.addGuess(entity, lakes.get(limit), false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("wrong");
  }

  @Test
  void moreCorrectGuessesThanAnswersIsRejected() {
    GameRoundEntity entity = guessRound(GameType.RIVERS_BY_SHAPE, AnswerState.UNANSWERED);
    entity.addGuess(new RoundGuess("Rhine", true));

    assertThatThrownBy(() -> rules.addGuess(entity, "Rhein", true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("correct");
  }

  @Test
  void aGuessOnAnAnsweredRoundIsAConflict() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.CORRECT);

    assertThatThrownBy(() -> rules.addGuess(entity, "France", false))
        .isInstanceOf(RoundAlreadyAnsweredForGuessException.class);
  }

  @Test
  void aRiverRoundAcceptsARiverAndRejectsACountry() {
    GameRoundEntity entity = guessRound(GameType.RIVERS_BY_SHAPE, AnswerState.UNANSWERED);

    rules.addGuess(entity, "rhein", false);
    assertThatThrownBy(() -> rules.addGuess(entity, "France", false))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(entity.getGuesses()).extracting(RoundGuess::getGuess).containsExactly("Rhein");
  }

  // ------------------------------------------------------------- addHint

  @Test
  void hintsAreRecordedInOrderWithTheGuessCountAtTheTime() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    rules.addGuess(entity, "France", false);

    rules.addHint(entity, 0, 0);
    rules.addHint(entity, 1, 1);

    assertThat(entity.getHintsTakenAt()).containsExactly(0, 1);
  }

  @Test
  void aSkippedHintAHintPastTheCountAndANegativeCountAreRejected() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);

    assertThatThrownBy(() -> rules.addHint(entity, 1, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> rules.addHint(entity, 0, -1))
        .isInstanceOf(IllegalArgumentException.class);
    for (int i = 0; i < GameType.TRIPOINT.hintCount(); i++) {
      rules.addHint(entity, i, 0);
    }
    int past = GameType.TRIPOINT.hintCount();
    assertThatThrownBy(() -> rules.addHint(entity, past, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("hints");
    assertThat(entity.getHintsTakenAt()).hasSize(past);
  }

  @Test
  void aGuessCountLargerThanTheStoredGuessesIsClamped() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.UNANSWERED);
    rules.addGuess(entity, "France", false);

    rules.addHint(entity, 0, 4);

    assertThat(entity.getHintsTakenAt()).containsExactly(1);
  }

  @Test
  void aHintOnAnAnsweredRoundIsAConflict() {
    GameRoundEntity entity = guessRound(GameType.TRIPOINT, AnswerState.GAVE_UP);

    assertThatThrownBy(() -> rules.addHint(entity, 0, 0))
        .isInstanceOf(RoundAlreadyAnsweredForGuessException.class);
  }
}
