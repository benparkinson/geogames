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
    "0, 0, 100, 0",
    "1, 0, 100, 0",
    "1, 1, 50, 0",
    "2, 1, 75, 0",
    "2, 2, 50, 0",
    "3, 1, 83, 0",
    "3, 2, 67, 0",
    "3, 3, 50, 0"
  })
  void roundPoints(int hintCount, int hintsUsed, int correct, int gaveUp) {
    assertThat(Scoring.roundPoints(AnswerState.CORRECT, hintsUsed, hintCount)).isEqualTo(correct);
    assertThat(Scoring.roundPoints(AnswerState.GAVE_UP, hintsUsed, hintCount)).isEqualTo(gaveUp);
    assertThat(Scoring.roundPoints(AnswerState.UNANSWERED, hintsUsed, hintCount)).isZero();
  }

  @Test
  void outOfGuessesScoresNothing() {
    assertThat(Scoring.roundPoints(AnswerState.OUT_OF_GUESSES, 1, 2)).isZero();
  }

  @Test
  void negativeHintsUsedIsRejected() {
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.CORRECT, -1, 2))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void negativeHintCountIsRejected() {
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.CORRECT, 0, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void moreHintsUsedThanThereAreIsRejected() {
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.CORRECT, 3, 2))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
