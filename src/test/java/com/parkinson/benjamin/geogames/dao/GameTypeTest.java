package com.parkinson.benjamin.geogames.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GameTypeTest {

  @Test
  void valuesContainsExactlyTheKnownTypes() {
    assertThat(GameType.values()).containsExactly(GameType.TRIPOINT, GameType.RIVERS_BY_SHAPE);
  }

  @Test
  void answerCounts() {
    assertThat(GameType.TRIPOINT.getAnswerCount()).isEqualTo(3);
    assertThat(GameType.RIVERS_BY_SHAPE.getAnswerCount()).isEqualTo(1);
  }

  @Test
  void namesAreUnique() {
    assertThat(Arrays.stream(GameType.values()).map(GameType::getName).distinct().count())
        .isEqualTo(GameType.values().length);
  }
}
