package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameType;

/**
 * A game type as the client sees it: {@code {"name": "TRIPOINT", "answerCount": 3, "hintCount":
 * 3, "guessLimit": 5}}. The hint count and guess limit are the game's own, for its difficulty.
 */
public record GameTypeResponse(String name, int answerCount, int hintCount, int guessLimit) {

  public static GameTypeResponse from(GameType gameType, Difficulty difficulty) {
    return new GameTypeResponse(
        gameType.name(),
        gameType.answerCount(),
        gameType.hintCount(difficulty),
        gameType.guessLimit(difficulty));
  }
}
