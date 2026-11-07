package com.parkinson.benjamin.geogames.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GameTypeConverterTest {

  private final GameTypeConverter converter = new GameTypeConverter();

  @Test
  void convertsToDatabaseName() {
    assertThat(converter.convertToDatabaseColumn(GameType.TRIPOINT)).isEqualTo("TRIPOINT");
    assertThat(converter.convertToDatabaseColumn(GameType.RIVERS_BY_SHAPE))
        .isEqualTo("RIVERS_BY_SHAPE");
  }

  @Test
  void roundTripsEveryGameTypeToTheSameInstance() {
    for (GameType type : GameType.values()) {
      assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(type)))
          .isSameAs(type);
    }
  }

  @Test
  void unknownAndNullNamesFallBackToRiversByShape() {
    // TODO: anything that is not "TRIPOINT" silently becomes RIVERS_BY_SHAPE, so a
    // corrupt or future game type name is never rejected.
    assertThat(converter.convertToEntityAttribute("SOMETHING_ELSE"))
        .isSameAs(GameType.RIVERS_BY_SHAPE);
    assertThat(converter.convertToEntityAttribute(null)).isSameAs(GameType.RIVERS_BY_SHAPE);
  }

  @Test
  void nullGameTypeCannotBeConverted() {
    // TODO: a null game type throws a NullPointerException instead of mapping to null.
    assertThatThrownBy(() -> converter.convertToDatabaseColumn(null))
        .isInstanceOf(NullPointerException.class);
  }
}
