package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
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
            GameType.RIVERS_BY_COUNTRIES,
            GameType.COUNTRIES_BY_NEIGHBOURS,
            GameType.COUNTRIES_BY_COASTLINE,
            GameType.COUNTRIES_BY_LAND_BORDERS,
            GameType.LAKES_BY_SHAPE,
            GameType.BORDERS_BY_SHAPE,
            GameType.MERGED_COUNTRIES,
            GameType.COUNTRIES_BY_CITIES,
            GameType.COUNTRIES_BY_RIVERS);
  }

  @Test
  void answerCounts() {
    assertThat(GameType.TRIPOINT.answerCount()).isEqualTo(3);
    assertThat(GameType.RIVERS_BY_SHAPE.answerCount()).isEqualTo(1);
    assertThat(GameType.RIVERS_BY_COUNTRIES.answerCount()).isEqualTo(1);
    assertThat(GameType.COUNTRIES_BY_NEIGHBOURS.answerCount()).isEqualTo(1);
    assertThat(GameType.COUNTRIES_BY_COASTLINE.answerCount()).isEqualTo(1);
    assertThat(GameType.COUNTRIES_BY_LAND_BORDERS.answerCount()).isEqualTo(1);
    assertThat(GameType.LAKES_BY_SHAPE.answerCount()).isEqualTo(1);
    assertThat(GameType.BORDERS_BY_SHAPE.answerCount()).isEqualTo(2);
    assertThat(GameType.MERGED_COUNTRIES.answerCount()).isEqualTo(2);
    assertThat(GameType.COUNTRIES_BY_CITIES.answerCount()).isEqualTo(1);
    assertThat(GameType.COUNTRIES_BY_RIVERS.answerCount()).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({
    "TRIPOINT, 3",
    "RIVERS_BY_SHAPE, 3",
    "RIVERS_BY_COUNTRIES, 2",
    "COUNTRIES_BY_NEIGHBOURS, 3",
    "COUNTRIES_BY_COASTLINE, 3",
    "COUNTRIES_BY_LAND_BORDERS, 3",
    "LAKES_BY_SHAPE, 3",
    "BORDERS_BY_SHAPE, 3",
    "MERGED_COUNTRIES, 3",
    "COUNTRIES_BY_CITIES, 3",
    "COUNTRIES_BY_RIVERS, 3"
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
    assertThat(GameType.RIVERS_BY_COUNTRIES.name()).isEqualTo("RIVERS_BY_COUNTRIES");
    assertThat(GameType.COUNTRIES_BY_NEIGHBOURS.name()).isEqualTo("COUNTRIES_BY_NEIGHBOURS");
    assertThat(GameType.COUNTRIES_BY_COASTLINE.name()).isEqualTo("COUNTRIES_BY_COASTLINE");
    assertThat(GameType.COUNTRIES_BY_LAND_BORDERS.name()).isEqualTo("COUNTRIES_BY_LAND_BORDERS");
    assertThat(GameType.LAKES_BY_SHAPE.name()).isEqualTo("LAKES_BY_SHAPE");
    assertThat(GameType.BORDERS_BY_SHAPE.name()).isEqualTo("BORDERS_BY_SHAPE");
    assertThat(GameType.MERGED_COUNTRIES.name()).isEqualTo("MERGED_COUNTRIES");
    assertThat(GameType.COUNTRIES_BY_CITIES.name()).isEqualTo("COUNTRIES_BY_CITIES");
    assertThat(GameType.COUNTRIES_BY_RIVERS.name()).isEqualTo("COUNTRIES_BY_RIVERS");
  }

  @Test
  void randomIsARequestOnlyValueNotAGameType() {
    for (GameType type : GameType.values()) {
      assertThat(type.name()).isNotEqualTo("RANDOM");
    }
  }

  /**
   * The daily challenge never uses these. Keep in step with {@code registry.test.ts}, which lists
   * the frontend modules with {@code extraDifficult: true}.
   */
  @Test
  void theExtraDifficultTypesAreRiverLakeAndBorderShapes() {
    assertThat(Arrays.stream(GameType.values()).filter(GameType::extraDifficult))
        .containsExactly(
            GameType.RIVERS_BY_SHAPE, GameType.LAKES_BY_SHAPE, GameType.BORDERS_BY_SHAPE);
  }

  @Test
  void easyDoublesTheGuessLimitOfEveryType() {
    for (GameType type : GameType.values()) {
      assertThat(type.guessLimit(Difficulty.EASY)).isEqualTo(type.guessLimit() * 2);
      assertThat(type.guessLimit(Difficulty.NORMAL)).isEqualTo(type.guessLimit());
      assertThat(type.guessLimit(Difficulty.HARD)).isEqualTo(type.guessLimit());
    }
  }

  @Test
  void easyDropsTheContinentHintOfRiverAndLakeShapesOnly() {
    for (GameType type : GameType.values()) {
      boolean dropsOne = type == GameType.RIVERS_BY_SHAPE || type == GameType.LAKES_BY_SHAPE;
      assertThat(type.hintCount(Difficulty.EASY))
          .as(type.name())
          .isEqualTo(dropsOne ? type.hintCount() - 1 : type.hintCount());
      assertThat(type.hintCount(Difficulty.NORMAL)).isEqualTo(type.hintCount());
      assertThat(type.hintCount(Difficulty.HARD)).isEqualTo(type.hintCount());
    }
  }
}
