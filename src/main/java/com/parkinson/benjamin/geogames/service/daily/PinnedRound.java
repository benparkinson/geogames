package com.parkinson.benjamin.geogames.service.daily;

import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.List;
import tools.jackson.core.type.TypeReference;

/**
 * One round of a daily challenge, as stored: enough to rebuild it with {@code rebuild(roundKey)}.
 */
public record PinnedRound(GameType gameType, String roundKey) {

  /** For reading a stored list of rounds (daily challenges and friend challenges). */
  public static final TypeReference<List<PinnedRound>> LIST = new TypeReference<>() {};

  public static PinnedRound of(GameData data) {
    return new PinnedRound(data.gameType(), data.roundKey());
  }
}
