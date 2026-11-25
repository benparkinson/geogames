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
   * The difficulty a request asks for: {@code difficulty} if present (a name that is not one of the
   * three is an {@link BadRequestException}, a 400), otherwise HARD for {@code hardMode} true
   * and NORMAL for false, otherwise null (unknown).
   */
  public static Difficulty resolve(String difficulty, Boolean hardMode) {
    if (difficulty != null) {
      try {
        return valueOf(difficulty);
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Unknown difficulty: " + difficulty);
      }
    }
    if (hardMode == null) {
      return null;
    }
    return hardMode ? HARD : NORMAL;
  }

  /** The difficulty to play by: unknown (null) is NORMAL. */
  public static Difficulty orNormal(Difficulty difficulty) {
    return difficulty == null ? NORMAL : difficulty;
  }
}
