package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.model.GameType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class GameSelectionTest {

  @Test
  void parsesEveryGameTypeByName() {
    for (GameType type : GameType.values()) {
      GameSelection selection = GameSelection.parse(type.name());

      assertThat(selection.isRandom()).isFalse();
      assertThat(selection.type()).isSameAs(type);
    }
  }

  @Test
  void parsesRandom() {
    GameSelection selection = GameSelection.parse("RANDOM");

    assertThat(selection.isRandom()).isTrue();
    assertThat(selection).isSameAs(GameSelection.RANDOM);
  }

  @Test
  void aRandomSelectionHasNoSingleType() {
    assertThatThrownBy(GameSelection.RANDOM::type).isInstanceOf(IllegalStateException.class);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"FOO", "tripoint", "Random", "", " TRIPOINT"})
  void rejectsAnythingElse(String name) {
    assertThatThrownBy(() -> GameSelection.parse(name))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown game type");
  }
}
