package com.parkinson.benjamin.geogames.model;

/**
 * How rounds are scored. The client has the same formula in {@code games/common/scoring.ts}; the
 * two share a test table and must agree.
 */
public final class Scoring {

  public static final int ANSWER_POINTS = 4;
  public static final int NO_HINT_POINTS = 3;
  public static final int NO_MISS_POINTS = 3;
  public static final int MAX_ROUND_POINTS = ANSWER_POINTS + NO_HINT_POINTS + NO_MISS_POINTS;

  private Scoring() {}

  /**
   * The points a round earns, out of {@value #MAX_ROUND_POINTS}: 4 for the answer, 3 for no hints
   * (1 off per hint) and 3 for no misses (1 off per wrong guess). A correct round always earns at
   * least the answer's 4. A revealed round ({@code GAVE_UP} or {@code OUT_OF_GUESSES}) earns the
   * share of the answer and no-hint points that was found, {@code answersFound} of {@code
   * answerCount} answers, rounded half up, and nothing for no misses. An unanswered round earns 0.
   */
  public static int roundPoints(
      AnswerState answerState, int hintsUsed, int answersFound, int answerCount, int wrongGuesses) {
    if (hintsUsed < 0 || wrongGuesses < 0) {
      throw new IllegalArgumentException(
          "%d hints and %d wrong guesses are not possible".formatted(hintsUsed, wrongGuesses));
    }
    if (answerCount < 1 || answersFound < 0 || answersFound > answerCount) {
      throw new IllegalArgumentException(
          "%d answers found is not possible with %d answers".formatted(answersFound, answerCount));
    }
    if (answerState == AnswerState.UNANSWERED) {
      return 0;
    }
    int noHints = Math.max(0, NO_HINT_POINTS - hintsUsed);
    if (answerState == AnswerState.CORRECT) {
      return ANSWER_POINTS + noHints + Math.max(0, NO_MISS_POINTS - wrongGuesses);
    }
    int full = ANSWER_POINTS + noHints;
    // round_half_up(full * answersFound / answerCount), in integers
    return (2 * full * answersFound + answerCount) / (2 * answerCount);
  }
}
