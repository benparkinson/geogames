package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ContinentTest {

  @Test
  void theLabelsAreTheSixNamesInOrder() {
    assertThat(Arrays.stream(Continent.values()).map(Continent::label))
        .containsExactly("Africa", "Asia", "Europe", "North America", "South America", "Oceania");
  }

  @Test
  void aLabelMapsBackToItsContinent() {
    for (Continent continent : Continent.values()) {
      assertThat(Continent.fromLabel(continent.label())).isEqualTo(continent);
    }
  }

  @Test
  void anUnknownLabelIsRejectedIncludingAustralia() {
    assertThatThrownBy(() -> Continent.fromLabel("Australia"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Australia");
    assertThatThrownBy(() -> Continent.fromLabel(null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
