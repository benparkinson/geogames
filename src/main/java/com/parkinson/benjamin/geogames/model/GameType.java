package com.parkinson.benjamin.geogames.model;

/**
 * The kinds of game a round can be. Stored by name in the database and sent to the client as a
 * {@link GameTypeResponse}, so renaming a constant changes both.
 */
public enum GameType {
  TRIPOINT(3, 3),
  RIVERS_BY_SHAPE(1, 1),
  RIVERS_ON_MAP(1, 2),
  RIVERS_BY_ENDS(1, 2),
  RIVERS_BY_COUNTRIES(1, 2);

  private final int answerCount;
  private final int hintCount;

  GameType(int answerCount, int hintCount) {
    this.answerCount = answerCount;
    this.hintCount = hintCount;
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
}
