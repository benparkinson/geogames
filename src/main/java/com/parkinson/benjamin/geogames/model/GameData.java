package com.parkinson.benjamin.geogames.model;

public interface GameData {
  GameType gameType();

  /**
   * Identifies what the round is about, so that one game never asks about the same thing twice,
   * even across game types (for example a river by shape and the same river on the map).
   */
  String subjectId();

  /**
   * Names this round's data exactly, so it can be stored instead of the data and rebuilt by the
   * round's generator. Unlike subjectId(), it is unique within a game type.
   */
  default String roundKey() {
    return subjectId();
  }
}
