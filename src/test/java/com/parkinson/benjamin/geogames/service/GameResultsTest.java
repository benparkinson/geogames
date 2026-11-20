package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.TestFixtures.game;
import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.api.GameResult;
import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.RoundGuess;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.GameType;
import org.junit.jupiter.api.Test;

class GameResultsTest {

  private static GameRoundEntity round(int index, AnswerState state) {
    GameRoundEntity entity = new GameRoundEntity(index, GameType.TRIPOINT, "{}");
    entity.recordAnswer(state, entity.getHintsUsed(), entity.getWrongGuesses(), null);
    return entity;
  }

  private static GameRoundEntity hintedRound(
      int index, GameType type, AnswerState state, int hintsUsed) {
    GameRoundEntity entity = new GameRoundEntity(index, type, "{}");
    entity.recordAnswer(state, hintsUsed, entity.getWrongGuesses(), null);
    return entity;
  }

  @Test
  void roundAnswerStatesAreOrderedByIndexAndScored() {
    GameEntity game =
        game(
            round(2, AnswerState.UNANSWERED),
            round(0, AnswerState.CORRECT),
            round(1, AnswerState.GAVE_UP));

    GameResult result = GameResults.of(game);

    assertThat(result.roundAnswerStates())
        .containsExactly(AnswerState.CORRECT, AnswerState.GAVE_UP, AnswerState.UNANSWERED);
    assertThat(result.score()).isEqualTo(100);
    assertThat(result.roundsUnanswered()).isEqualTo(1);
    assertThat(result.gameState()).isEqualTo(GameState.IN_PROGRESS);
  }

  @Test
  void gameIsFinishedWhenEveryRoundIsAnswered() {
    GameEntity game = game(round(1, AnswerState.GAVE_UP), round(0, AnswerState.CORRECT));

    GameResult result = GameResults.of(game);

    assertThat(result.gameState()).isEqualTo(GameState.FINISHED);
    assertThat(result.roundsUnanswered()).isZero();
    assertThat(result.roundAnswerStates())
        .containsExactly(AnswerState.CORRECT, AnswerState.GAVE_UP);
  }

  @Test
  void gameIsCreatedWhenNothingIsAnswered() {
    GameEntity game = game(round(0, AnswerState.UNANSWERED), round(1, AnswerState.UNANSWERED));

    GameResult result = GameResults.of(game);

    assertThat(result.gameState()).isEqualTo(GameState.CREATED);
    assertThat(result.roundsUnanswered()).isEqualTo(2);
    assertThat(result.score()).isZero();
  }

  @Test
  void scoreIsTheSumOfRoundPoints() {
    GameEntity game =
        game(
            hintedRound(3, GameType.TRIPOINT, AnswerState.GAVE_UP, 2),
            hintedRound(1, GameType.RIVERS_BY_COUNTRIES, AnswerState.CORRECT, 1),
            hintedRound(0, GameType.TRIPOINT, AnswerState.CORRECT, 0),
            hintedRound(2, GameType.TRIPOINT, AnswerState.CORRECT, 1),
            hintedRound(4, GameType.RIVERS_BY_SHAPE, AnswerState.UNANSWERED, 0));

    GameResult result = GameResults.of(game);

    // perfect 100 + river with 1 of 2 hints 75 + tripoint with 1 of 3 hints 83 + revealed 0
    assertThat(result.roundPoints()).containsExactly(100, 75, 83, 0, 0);
    assertThat(result.roundHintsUsed()).containsExactly(0, 1, 1, 2, 0);
    assertThat(result.score()).isEqualTo(258);
    assertThat(result.maxScore()).isEqualTo(500);
    assertThat(result.roundsCorrect()).isEqualTo(3);
    assertThat(result.perfectRounds()).isEqualTo(1);
  }

  @Test
  void revealedRoundsScoreNothing() {
    GameEntity game = game(round(0, AnswerState.GAVE_UP), round(1, AnswerState.GAVE_UP));

    GameResult result = GameResults.of(game);

    assertThat(result.score()).isZero();
    assertThat(result.maxScore()).isEqualTo(200);
  }

  @Test
  void aRevealedRoundScoresItsShareOfTheAnswersFound() {
    GameRoundEntity partial = new GameRoundEntity(0, GameType.TRIPOINT, "{}");
    partial.recordAnswer(AnswerState.GAVE_UP, 0, null, 2);
    GameRoundEntity none = new GameRoundEntity(1, GameType.TRIPOINT, "{}");
    none.recordAnswer(AnswerState.GAVE_UP, 0, null, null);

    GameResult result = GameResults.of(game(partial, none));

    assertThat(result.roundPoints()).containsExactly(67, 0);
    assertThat(result.score()).isEqualTo(67);
    assertThat(result.roundsCorrect()).isZero();
    assertThat(result.perfectRounds()).isZero();
  }

  private static GameRoundEntity answered(
      AnswerState state, int hintsUsed, Integer wrongGuesses, boolean... recordedGuesses) {
    GameRoundEntity entity = new GameRoundEntity(0, GameType.TRIPOINT, "{}");
    for (boolean correct : recordedGuesses) {
      entity.addGuess(new RoundGuess("Peru", correct));
    }
    entity.recordAnswer(state, hintsUsed, wrongGuesses, null);
    return entity;
  }

  @Test
  void aCorrectRoundWithNoHintsAndNoWrongGuessIsAFirstGuess() {
    assertThat(GameResults.isFirstGuess(answered(AnswerState.CORRECT, 0, 0, true, true))).isTrue();
    assertThat(GameResults.isFirstGuess(answered(AnswerState.CORRECT, 0, 0))).isTrue();
  }

  @Test
  void aRoundIsNotAFirstGuessWithAHintAWrongGuessOrAnUnknownCount() {
    assertThat(GameResults.isFirstGuess(answered(AnswerState.CORRECT, 1, 0))).isFalse();
    assertThat(GameResults.isFirstGuess(answered(AnswerState.CORRECT, 0, 1))).isFalse();
    assertThat(GameResults.isFirstGuess(answered(AnswerState.CORRECT, 0, null))).isFalse();
  }

  @Test
  void aRecordedWrongGuessMeansNotFirstGuessWhateverWasReported() {
    assertThat(GameResults.isFirstGuess(answered(AnswerState.CORRECT, 0, 0, false, true)))
        .isFalse();
  }

  @Test
  void aMissedRoundIsNeverAFirstGuess() {
    assertThat(GameResults.isFirstGuess(answered(AnswerState.GAVE_UP, 0, 0))).isFalse();
    assertThat(GameResults.isFirstGuess(answered(AnswerState.OUT_OF_GUESSES, 0, 0))).isFalse();
    assertThat(GameResults.isFirstGuess(answered(AnswerState.UNANSWERED, 0, 0))).isFalse();
  }

  @Test
  void firstGuessRoundsAreListedInRoundOrderAndCounted() {
    GameRoundEntity first = answered(AnswerState.CORRECT, 0, 0);
    GameRoundEntity second = new GameRoundEntity(1, GameType.TRIPOINT, "{}");
    second.recordAnswer(AnswerState.CORRECT, 0, 2, null);
    GameRoundEntity third = new GameRoundEntity(2, GameType.TRIPOINT, "{}");
    third.recordAnswer(AnswerState.GAVE_UP, 0, 0, null);

    GameResult result = GameResults.of(game(third, first, second));

    assertThat(result.roundFirstGuess()).containsExactly(true, false, false);
    assertThat(result.firstGuessRounds()).isEqualTo(1);
  }

  @Test
  void partialCreditTakesTheHintsIntoAccount() {
    GameRoundEntity partial = new GameRoundEntity(0, GameType.TRIPOINT, "{}");
    partial.recordAnswer(AnswerState.OUT_OF_GUESSES, 1, 5, 1);

    assertThat(GameResults.pointsFor(partial)).isEqualTo(28);
  }
}
