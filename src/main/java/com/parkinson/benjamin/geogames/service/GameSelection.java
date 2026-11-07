package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.GameType;

/**
 * What the player asked for when creating a game: one specific {@link GameType}, or a random mix
 * of all of them. {@code RANDOM} only exists in requests, so it is not a {@link GameType}.
 */
public final class GameSelection {

  private static final String RANDOM_NAME = "RANDOM";

  public static final GameSelection RANDOM = new GameSelection(null);

  private final GameType type;

  private GameSelection(GameType type) {
    this.type = type;
  }

  /** Parses the request's game type name, which is case sensitive. */
  public static GameSelection parse(String name) {
    if (RANDOM_NAME.equals(name)) {
      return RANDOM;
    }
    for (GameType type : GameType.values()) {
      if (type.name().equals(name)) {
        return new GameSelection(type);
      }
    }
    throw new IllegalArgumentException("Unknown game type %s!".formatted(name));
  }

  public boolean isRandom() {
    return type == null;
  }

  /** The chosen type. Only valid when {@link #isRandom()} is false. */
  public GameType type() {
    if (type == null) {
      throw new IllegalStateException("A random selection has no single game type");
    }
    return type;
  }
}
