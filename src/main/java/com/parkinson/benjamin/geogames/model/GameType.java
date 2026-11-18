package com.parkinson.benjamin.geogames.model;

/**
 * The kinds of game a round can be. Stored by name in the database and sent to the client as a
 * {@link GameTypeResponse}, so renaming a constant changes both.
 */
public enum GameType {
  TRIPOINT(3, 3, 5, PlaceKind.COUNTRIES, false),
  RIVERS_BY_SHAPE(1, 3, 5, PlaceKind.RIVERS, true),
  RIVERS_BY_COUNTRIES(1, 2, 5, PlaceKind.RIVERS, false),
  COUNTRIES_BY_NEIGHBOURS(1, 3, 5, PlaceKind.COUNTRIES, false),
  COUNTRIES_BY_COASTLINE(1, 3, 5, PlaceKind.COUNTRIES, false),
  COUNTRIES_BY_LAND_BORDERS(1, 3, 5, PlaceKind.COUNTRIES, false),
  LAKES_BY_SHAPE(1, 3, 5, PlaceKind.LAKES, true),
  BORDERS_BY_SHAPE(2, 3, 5, PlaceKind.COUNTRIES, true),
  MERGED_COUNTRIES(2, 3, 5, PlaceKind.COUNTRIES, false),
  COUNTRIES_BY_CITIES(1, 3, 5, PlaceKind.COUNTRIES, false),
  COUNTRIES_BY_RIVERS(1, 3, 5, PlaceKind.COUNTRIES, false);

  private final int answerCount;
  private final int hintCount;
  private final int guessLimit;
  private final PlaceKind placeKind;
  private final boolean extraDifficult;

  GameType(
      int answerCount, int hintCount, int guessLimit, PlaceKind placeKind, boolean extraDifficult) {
    if (guessLimit < 1) {
      throw new IllegalArgumentException("guessLimit must be at least 1: " + guessLimit);
    }
    this.answerCount = answerCount;
    this.hintCount = hintCount;
    this.guessLimit = guessLimit;
    this.placeKind = placeKind;
    this.extraDifficult = extraDifficult;
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

  /** Which list of names the player's guesses in a round of this type come from. */
  public PlaceKind placeKind() {
    return placeKind;
  }

  /**
   * The "Extra difficult" types, which the daily challenge never uses. Keep in step with {@code
   * extraDifficult: true} in the frontend game modules ({@code registry.test.ts} holds the same
   * list).
   */
  public boolean extraDifficult() {
    return extraDifficult;
  }
}
