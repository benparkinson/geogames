package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GameTypeTest {

  @Test
  void valuesContainsExactlyTheKnownTypes() {
    assertThat(GameType.values())
        .containsExactly(
            GameType.TRIPOINT,
            GameType.RIVERS_BY_SHAPE,
            GameType.RIVERS_ON_MAP,
            GameType.RIVERS_BY_ENDS,
            GameType.RIVERS_BY_COUNTRIES,
            GameType.COUNTRIES_BY_NEIGHBOURS);
  }

  @Test
  void answerCounts() {
    assertThat(GameType.TRIPOINT.answerCount()).isEqualTo(3);
    assertThat(GameType.RIVERS_BY_SHAPE.answerCount()).isEqualTo(1);
    assertThat(GameType.RIVERS_ON_MAP.answerCount()).isEqualTo(1);
    assertThat(GameType.RIVERS_BY_ENDS.answerCount()).isEqualTo(1);
    assertThat(GameType.RIVERS_BY_COUNTRIES.answerCount()).isEqualTo(1);
    assertThat(GameType.COUNTRIES_BY_NEIGHBOURS.answerCount()).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({
    "TRIPOINT, 3",
    "RIVERS_BY_SHAPE, 3",
    "RIVERS_ON_MAP, 2",
    "RIVERS_BY_ENDS, 2",
    "RIVERS_BY_COUNTRIES, 2",
    "COUNTRIES_BY_NEIGHBOURS, 3"
  })
  void hintCounts(GameType type, int hintCount) {
    assertThat(type.hintCount()).isEqualTo(hintCount);
  }

  @Test
  void everyTypeHasANonNegativeHintCount() {
    for (GameType type : GameType.values()) {
      assertThat(type.hintCount()).isGreaterThanOrEqualTo(0);
    }
  }

  @Test
  void everyTypeAllowsAtLeastOneWrongGuess() {
    for (GameType type : GameType.values()) {
      assertThat(type.guessLimit()).isGreaterThanOrEqualTo(1);
    }
  }

  @Test
  void namesAreTheStableWireAndDatabaseNames() {
    assertThat(GameType.TRIPOINT.name()).isEqualTo("TRIPOINT");
    assertThat(GameType.RIVERS_BY_SHAPE.name()).isEqualTo("RIVERS_BY_SHAPE");
    assertThat(GameType.RIVERS_ON_MAP.name()).isEqualTo("RIVERS_ON_MAP");
    assertThat(GameType.RIVERS_BY_ENDS.name()).isEqualTo("RIVERS_BY_ENDS");
    assertThat(GameType.RIVERS_BY_COUNTRIES.name()).isEqualTo("RIVERS_BY_COUNTRIES");
    assertThat(GameType.COUNTRIES_BY_NEIGHBOURS.name()).isEqualTo("COUNTRIES_BY_NEIGHBOURS");
  }

  @Test
  void randomIsARequestOnlyValueNotAGameType() {
    for (GameType type : GameType.values()) {
      assertThat(type.name()).isNotEqualTo("RANDOM");
    }
  }
}
