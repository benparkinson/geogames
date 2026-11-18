package com.parkinson.benjamin.geogames.model;

/** The data for one round of the lake game. */
public record LakeRound(GameType gameType, Lake lake) implements GameData {

  @Override
  public String subjectId() {
    return SubjectIds.lake(lake.id());
  }
}
