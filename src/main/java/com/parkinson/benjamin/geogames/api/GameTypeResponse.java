package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.GameType;

/** A game type as the client sees it: {@code {"name": "TRIPOINT", "answerCount": 3}}. */
public record GameTypeResponse(String name, int answerCount) {

  public static GameTypeResponse from(GameType gameType) {
    return new GameTypeResponse(gameType.name(), gameType.answerCount());
  }
}
