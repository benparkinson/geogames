package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DifficultyTest {

  @Test
  void aNamedDifficultyWinsOverHardMode() {
    assertThat(Difficulty.resolve("EASY", null)).isEqualTo(Difficulty.EASY);
    assertThat(Difficulty.resolve("EASY", true)).isEqualTo(Difficulty.EASY);
    assertThat(Difficulty.resolve("NORMAL", true)).isEqualTo(Difficulty.NORMAL);
    assertThat(Difficulty.resolve("HARD", false)).isEqualTo(Difficulty.HARD);
  }

  @Test
  void hardModeAloneMapsToHardOrNormal() {
    assertThat(Difficulty.resolve(null, true)).isEqualTo(Difficulty.HARD);
    assertThat(Difficulty.resolve(null, false)).isEqualTo(Difficulty.NORMAL);
  }

  @Test
  void neitherIsUnknown() {
    assertThat(Difficulty.resolve(null, null)).isNull();
  }

  @Test
  void aBadNameIsRejectedEvenWithHardMode() {
    assertThatThrownBy(() -> Difficulty.resolve("easy", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("easy");
    assertThatThrownBy(() -> Difficulty.resolve("", true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void unknownPlaysAsNormal() {
    assertThat(Difficulty.orNormal(null)).isEqualTo(Difficulty.NORMAL);
    assertThat(Difficulty.orNormal(Difficulty.HARD)).isEqualTo(Difficulty.HARD);
  }
}
