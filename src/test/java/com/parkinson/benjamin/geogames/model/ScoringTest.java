package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScoringTest {

  /** The same table is in games/common/scoring.test.ts. Keep them identical. */
  @ParameterizedTest
  @CsvSource({
    "0, 0, 10",
    "0, 1, 9",
    "0, 3, 7",
    "0, 6, 7",
    "1, 0, 9",
    "3, 0, 7",
    "2, 2, 6",
    "3, 4, 4"
  })
  void correctRounds(int hintsUsed, int wrongGuesses, int points) {
    assertThat(Scoring.roundPoints(AnswerState.CORRECT, hintsUsed, 1, 1, wrongGuesses))
        .isEqualTo(points);
  }

  /** The same table is in games/common/scoring.test.ts. Keep them identical. */
  @ParameterizedTest
  @CsvSource({
    "0, 1, 3, 2, 5",
    "0, 1, 3, 1, 2",
    "0, 1, 2, 1, 4",
    "0, 1, 3, 0, 0",
    "3, 1, 3, 1, 1",
    "1, 1, 2, 1, 3"
  })
  void revealedRounds(int hintsUsed, int wrongGuesses, int answerCount, int found, int points) {
    assertThat(
            Scoring.roundPoints(AnswerState.GAVE_UP, hintsUsed, found, answerCount, wrongGuesses))
        .isEqualTo(points);
    assertThat(
            Scoring.roundPoints(
                AnswerState.OUT_OF_GUESSES, hintsUsed, found, answerCount, wrongGuesses))
        .isEqualTo(points);
  }

  @Test
  void anUnansweredRoundScoresNothingWhateverWasFound() {
    assertThat(Scoring.roundPoints(AnswerState.UNANSWERED, 0, 2, 3, 0)).isZero();
  }

  @Test
  void aCorrectRoundIgnoresTheFoundCount() {
    assertThat(Scoring.roundPoints(AnswerState.CORRECT, 1, 0, 3, 0)).isEqualTo(9);
  }

  @Test
  void aRevealedRoundEarnsNothingForNoMisses() {
    assertThat(Scoring.roundPoints(AnswerState.GAVE_UP, 0, 1, 1, 0)).isEqualTo(7);
  }

  @Test
  void negativeCountsAreRejected() {
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.CORRECT, -1, 1, 1, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.CORRECT, 0, 1, 1, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void anImpossibleFoundCountIsRejected() {
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.GAVE_UP, 0, -1, 3, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.GAVE_UP, 0, 4, 3, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.GAVE_UP, 0, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
