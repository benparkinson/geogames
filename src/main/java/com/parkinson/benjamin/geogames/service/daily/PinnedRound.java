package com.parkinson.benjamin.geogames.service.daily;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;

/**
 * One round of a daily challenge, as stored: enough to rebuild it with {@code rebuild(roundKey)}.
 */
public record PinnedRound(GameType gameType, String roundKey) {

  public static PinnedRound of(GameData data) {
    return new PinnedRound(data.gameType(), data.roundKey());
  }
}
