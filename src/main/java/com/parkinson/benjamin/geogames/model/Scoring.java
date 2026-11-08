package com.parkinson.benjamin.geogames.model;

/**
 * How rounds are scored. The client has the same formula in {@code games/common/scoring.ts}; the two
 * share a test table and must agree.
 */
public final class Scoring {

  public static final int MAX_ROUND_POINTS = 100;
  public static final int MAX_HINT_PENALTY = 50;

  private Scoring() {}

  /** The points a round earns: 100 if perfect, less for each hint, 0 unless correct. */
  public static int roundPoints(AnswerState answerState, int hintsUsed, int hintCount) {
    if (hintsUsed < 0 || hintCount < 0 || hintsUsed > hintCount) {
      throw new IllegalArgumentException(
          "%d hints used is not possible with %d hints".formatted(hintsUsed, hintCount));
    }
    if (answerState != AnswerState.CORRECT) {
      return 0;
    }
    if (hintCount == 0) {
      return MAX_ROUND_POINTS;
    }
    // round_half_up(MAX_HINT_PENALTY * hintsUsed / hintCount), in integers
    int penalty = (2 * MAX_HINT_PENALTY * hintsUsed + hintCount) / (2 * hintCount);
    return MAX_ROUND_POINTS - penalty;
  }
}
