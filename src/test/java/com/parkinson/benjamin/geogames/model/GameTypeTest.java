package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GameTypeTest {

  @Test
  void valuesContainsExactlyTheKnownTypes() {
    assertThat(GameType.values()).containsExactly(GameType.TRIPOINT, GameType.RIVERS_BY_SHAPE);
  }

  @Test
  void answerCounts() {
    assertThat(GameType.TRIPOINT.answerCount()).isEqualTo(3);
    assertThat(GameType.RIVERS_BY_SHAPE.answerCount()).isEqualTo(1);
  }

  @Test
  void namesAreTheStableWireAndDatabaseNames() {
    assertThat(GameType.TRIPOINT.name()).isEqualTo("TRIPOINT");
    assertThat(GameType.RIVERS_BY_SHAPE.name()).isEqualTo("RIVERS_BY_SHAPE");
  }

  @Test
  void randomIsARequestOnlyValueNotAGameType() {
    for (GameType type : GameType.values()) {
      assertThat(type.name()).isNotEqualTo("RANDOM");
    }
  }
}
