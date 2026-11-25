package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DifficultyTest {

  @Test
  void aNamedDifficultyIsResolved() {
    assertThat(Difficulty.resolve("EASY")).isEqualTo(Difficulty.EASY);
    assertThat(Difficulty.resolve("NORMAL")).isEqualTo(Difficulty.NORMAL);
    assertThat(Difficulty.resolve("HARD")).isEqualTo(Difficulty.HARD);
  }

  @Test
  void noNameIsUnknown() {
    assertThat(Difficulty.resolve(null)).isNull();
  }

  @Test
  void aBadNameIsRejected() {
    assertThatThrownBy(() -> Difficulty.resolve("easy"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("easy");
    assertThatThrownBy(() -> Difficulty.resolve("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void unknownPlaysAsNormal() {
    assertThat(Difficulty.orNormal(null)).isEqualTo(Difficulty.NORMAL);
    assertThat(Difficulty.orNormal(Difficulty.HARD)).isEqualTo(Difficulty.HARD);
  }
}
