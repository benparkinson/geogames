package com.parkinson.benjamin.geogames.loaders.features;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContinentResolverTest {

  @Test
  void oneDirectValueWins() {
    assertThat(ContinentResolver.resolve(null, List.of("Africa"), List.of("Asia")))
        .contains("Africa");
  }

  @Test
  void theCountryContinentsAreTheFallback() {
    assertThat(
            ContinentResolver.resolve(null, List.of(), List.of("South America", "South America")))
        .contains("South America");
  }

  @Test
  void oceaniaMapsToOceania() {
    assertThat(ContinentResolver.resolve(null, List.of("Oceania"), List.of())).contains("Oceania");
    assertThat(ContinentResolver.map("Insular Oceania")).contains("Oceania");
    assertThat(ContinentResolver.map("Australian continent")).contains("Oceania");
  }

  @Test
  void europePlusAsiaIsUnresolved() {
    assertThat(ContinentResolver.resolve(null, List.of("Europe", "Asia"), List.of())).isEmpty();
    assertThat(ContinentResolver.resolve(null, List.of(), List.of("Europe", "Asia"))).isEmpty();
  }

  @Test
  void unknownLabelsAreIgnored() {
    assertThat(ContinentResolver.resolve(null, List.of("Eurasia", "Antarctica"), List.of("Asia")))
        .contains("Asia");
  }

  @Test
  void anOverrideWinsOverEverything() {
    assertThat(ContinentResolver.resolve("Asia", List.of("Europe"), List.of("Africa")))
        .contains("Asia");
  }
}
