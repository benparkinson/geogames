package com.parkinson.benjamin.geogames.model;

/**
 * How rounds are scored. The client has the same formula in {@code games/common/scoring.ts}; the
 * two share a test table and must agree.
 */
public final class Scoring {

  public static final int MAX_ROUND_POINTS = 100;
  public static final int MAX_HINT_PENALTY = 50;

  private Scoring() {}

  /**
   * The points a round earns. A correct round earns 100 less the hint penalty. A revealed round
   * ({@code GAVE_UP} or {@code OUT_OF_GUESSES}) earns the share of that which was found, {@code
   * answersFound} of {@code answerCount} answers, rounded half up; with none found that is 0. An
   * unanswered round earns 0.
   */
  public static int roundPoints(
      AnswerState answerState, int hintsUsed, int hintCount, int answersFound, int answerCount) {
    if (hintsUsed < 0 || hintCount < 0 || hintsUsed > hintCount) {
      throw new IllegalArgumentException(
          "%d hints used is not possible with %d hints".formatted(hintsUsed, hintCount));
    }
    if (answerCount < 1 || answersFound < 0 || answersFound > answerCount) {
      throw new IllegalArgumentException(
          "%d answers found is not possible with %d answers".formatted(answersFound, answerCount));
    }
    if (answerState == AnswerState.UNANSWERED) {
      return 0;
    }
    int full = fullPoints(hintsUsed, hintCount);
    if (answerState == AnswerState.CORRECT) {
      return full;
    }
    // round_half_up(full * answersFound / answerCount), in integers
    return (2 * full * answersFound + answerCount) / (2 * answerCount);
  }

  private static int fullPoints(int hintsUsed, int hintCount) {
    if (hintCount == 0) {
      return MAX_ROUND_POINTS;
    }
    // round_half_up(MAX_HINT_PENALTY * hintsUsed / hintCount), in integers
    int penalty = (2 * MAX_HINT_PENALTY * hintsUsed + hintCount) / (2 * hintCount);
    return MAX_ROUND_POINTS - penalty;
  }
}
