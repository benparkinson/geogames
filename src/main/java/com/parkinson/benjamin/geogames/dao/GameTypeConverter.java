package com.parkinson.benjamin.geogames.dao;

import jakarta.persistence.AttributeConverter;

public class GameTypeConverter implements AttributeConverter<GameType, String> {

  @Override
  public String convertToDatabaseColumn(GameType gameType) {
    return gameType.getName();
  }

  @Override
  public GameType convertToEntityAttribute(String s) {
    if (GameType.TRIPOINT.getName().equals(s)) {
      return GameType.TRIPOINT;
    }
    return GameType.RIVERS_BY_SHAPE;
  }
}
