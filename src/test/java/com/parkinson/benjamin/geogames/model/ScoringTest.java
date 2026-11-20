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
    assertThat(Scoring.roundPoints(AnswerState.CORRECT, hintsUsed, hintCount, 1, 1))
        .isEqualTo(correct);
    assertThat(Scoring.roundPoints(AnswerState.GAVE_UP, hintsUsed, hintCount, 0, 1))
        .isEqualTo(gaveUp);
    assertThat(Scoring.roundPoints(AnswerState.UNANSWERED, hintsUsed, hintCount, 0, 1)).isZero();
  }

  /** The same table is in games/common/scoring.test.ts. Keep them identical. */
  @ParameterizedTest
  @CsvSource({
    "3, 0, 3, 0, 0",
    "3, 1, 3, 0, 33",
    "3, 2, 3, 0, 67",
    "3, 1, 3, 1, 28",
    "3, 2, 3, 1, 55",
    "3, 1, 3, 2, 22",
    "3, 2, 3, 2, 45",
    "3, 1, 3, 3, 17",
    "3, 2, 3, 3, 33",
    "2, 1, 3, 0, 50",
    "2, 1, 3, 1, 42",
    "2, 1, 3, 2, 34",
    "2, 1, 3, 3, 25",
    "1, 0, 0, 0, 0"
  })
  void partialCredit(int answerCount, int found, int hintCount, int hintsUsed, int points) {
    assertThat(Scoring.roundPoints(AnswerState.GAVE_UP, hintsUsed, hintCount, found, answerCount))
        .isEqualTo(points);
    assertThat(
            Scoring.roundPoints(
                AnswerState.OUT_OF_GUESSES, hintsUsed, hintCount, found, answerCount))
        .isEqualTo(points);
  }

  @Test
  void aCorrectRoundIgnoresTheFoundCount() {
    assertThat(Scoring.roundPoints(AnswerState.CORRECT, 1, 3, 3, 3)).isEqualTo(83);
  }

  @Test
  void anUnansweredRoundScoresNothingWhateverWasFound() {
    assertThat(Scoring.roundPoints(AnswerState.UNANSWERED, 0, 3, 2, 3)).isZero();
  }

  @Test
  void negativeHintsUsedIsRejected() {
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.CORRECT, -1, 2, 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void negativeHintCountIsRejected() {
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.CORRECT, 0, -1, 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void moreHintsUsedThanThereAreIsRejected() {
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.CORRECT, 3, 2, 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void anImpossibleFoundCountIsRejected() {
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.GAVE_UP, 0, 3, -1, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.GAVE_UP, 0, 3, 4, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Scoring.roundPoints(AnswerState.GAVE_UP, 0, 3, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
