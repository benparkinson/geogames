package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.api.BadRequestException;

/**
 * How a game is played, chosen when it is created and never changed. Stored by name.
 *
 * <ul>
 *   <li>{@code EASY}: the continent is shown and every {@link GameType#guessLimit()} gets extra guesses.
 *   <li>{@code NORMAL}: the game as it is.
 *   <li>{@code HARD}: no distance or direction clues after a wrong guess (the client's job).
 * </ul>
 */
public enum Difficulty {
  EASY,
  NORMAL,
  HARD;

  /**
   * The difficulty a request asks for: null (unknown) when absent; a name that is not one of the
   * three is a {@link BadRequestException}, a 400.
   */
  public static Difficulty resolve(String difficulty) {
    if (difficulty == null) {
      return null;
    }
    try {
      return valueOf(difficulty);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Unknown difficulty: " + difficulty);
    }
  }

  /** The difficulty to play by: unknown (null) is NORMAL. */
  public static Difficulty orNormal(Difficulty difficulty) {
    return difficulty == null ? NORMAL : difficulty;
  }
}
