package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.GameType;

/**
 * A stored round key no longer matches any data: the map data changed in a deploy after the game
 * was created.
 */
public class RoundDataMissingException extends RuntimeException {

  public RoundDataMissingException(GameType gameType, String roundKey) {
    super("No %s data for round key '%s'".formatted(gameType, roundKey));
  }
}
