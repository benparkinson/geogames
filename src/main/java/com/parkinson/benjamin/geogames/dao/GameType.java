package com.parkinson.benjamin.geogames.dao;

public class GameType {

  public static final GameType TRIPOINT = new GameType("TRIPOINT", 3);
  public static final GameType RIVERS_BY_SHAPE = new GameType("RIVERS_BY_SHAPE", 1);

  public static GameType[] values() {
    return new GameType[] {TRIPOINT, RIVERS_BY_SHAPE};
  }

  private final String name;
  private final int answerCount;

  private GameType(String name, int answerCount) {
    this.name = name;
    this.answerCount = answerCount;
  }

  public String getName() {
    return name;
  }

  public int getAnswerCount() {
    return answerCount;
  }
}
