package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaceNamesTest {

  @Test
  void dropsAccentsCaseSpacesAndPunctuation() {
    assertThat(PlaceNames.normalise("  République  Française! ")).isEqualTo("republiquefrancaise");
    assertThat(PlaceNames.normalise("Côte d'Ivoire")).isEqualTo("cotedivoire");
  }

  @Test
  void aNameMadeOfOtherAlphabetsIsEmpty() {
    assertThat(PlaceNames.normalise("日本")).isEmpty();
  }
}
