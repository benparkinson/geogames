package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.River;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiverCountriesEligibilityTest {

  private static final Coordinate POINT = new Coordinate(1, 1);

  private static River river(String id, List<String> countries) {
    return new River(id, "River " + id, List.of(), "Asia", null, POINT, POINT, countries);
  }

  @Test
  void needsTwoCountries() {
    assertThat(RiverCountriesEligibility.MIN_COUNTRIES).isEqualTo(2);
  }

  @Test
  void rejectsNoCountriesOneCountryAndTheExcludedRivers() {
    var rule = RiverCountriesEligibility.riverCountries();

    assertThat(rule.test(river("A", null))).isFalse();
    assertThat(rule.test(river("A", List.of("X")))).isFalse();
    assertThat(rule.test(river("Q2251", List.of("X", "Y")))).isFalse();
    assertThat(rule.test(river("Q104437", List.of("X", "Y")))).isFalse();
    assertThat(rule.test(river("Q35591", List.of("X", "Y")))).isFalse();
  }

  @Test
  void acceptsTwoOrMoreCountries() {
    var rule = RiverCountriesEligibility.riverCountries();

    assertThat(rule.test(river("A", List.of("X", "Y")))).isTrue();
    assertThat(rule.test(river("A", List.of("X", "Y", "Z")))).isTrue();
  }
}
