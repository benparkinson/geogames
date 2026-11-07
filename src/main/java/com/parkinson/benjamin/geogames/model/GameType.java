package com.parkinson.benjamin.geogames.model;

/**
 * The kinds of game a round can be. Stored by name in the database and sent to the client as a
 * {@link GameTypeResponse}, so renaming a constant changes both.
 */
public enum GameType {
  TRIPOINT(3),
  RIVERS_BY_SHAPE(1),
  RIVERS_ON_MAP(1),
  RIVERS_BY_ENDS(1),
  RIVERS_BY_COUNTRIES(1);

  private final int answerCount;

  GameType(int answerCount) {
    this.answerCount = answerCount;
  }

  /** How many things the player has to name to get a round of this type right. */
  public int answerCount() {
    return answerCount;
  }
}
