package com.parkinson.benjamin.geogames.model;

public interface GameData {
  GameType gameType();

  /**
   * Identifies what the round is about, so that one game never asks about the same thing twice,
   * even across game types (for example a river by shape and the same river on the map).
   */
  String subjectId();
}
