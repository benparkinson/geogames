package com.parkinson.benjamin.geogames.model;

/**
 * The kinds of game a round can be. Stored by name in the database and sent to the client as a
 * {@link GameTypeResponse}, so renaming a constant changes both.
 */
public enum GameType {
  TRIPOINT(3, 3, 5),
  RIVERS_BY_SHAPE(1, 3, 5),
  RIVERS_ON_MAP(1, 2, 5),
  RIVERS_BY_ENDS(1, 2, 5),
  RIVERS_BY_COUNTRIES(1, 2, 5),
  COUNTRIES_BY_NEIGHBOURS(1, 3, 5),
  COUNTRIES_BY_COASTLINE(1, 3, 5),
  COUNTRIES_BY_LAND_BORDERS(1, 3, 5);

  private final int answerCount;
  private final int hintCount;
  private final int guessLimit;

  GameType(int answerCount, int hintCount, int guessLimit) {
    if (guessLimit < 1) {
      throw new IllegalArgumentException("guessLimit must be at least 1: " + guessLimit);
    }
    this.answerCount = answerCount;
    this.hintCount = hintCount;
    this.guessLimit = guessLimit;
  }

  /** How many things the player has to name to get a round of this type right. */
  public int answerCount() {
    return answerCount;
  }

  /**
   * How many hints the client offers for a round of this type. Using all of them costs {@code
   * Scoring.MAX_HINT_PENALTY} points.
   */
  public int hintCount() {
    return hintCount;
  }

  /**
   * How many wrong guesses a round of this type allows. The round is lost ({@code
   * AnswerState.OUT_OF_GUESSES}) when they are all used.
   */
  public int guessLimit() {
    return guessLimit;
  }
}
