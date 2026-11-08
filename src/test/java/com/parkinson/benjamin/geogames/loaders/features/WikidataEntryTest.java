package com.parkinson.benjamin.geogames.loaders.features;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class WikidataEntryTest {

  @Test
  void missingListsBecomeEmptyListsNeverNull() {
    WikidataEntry entry = new WikidataEntry("Nile", 10, null, null, null);

    assertThat(entry.aliases()).isEmpty();
    assertThat(entry.continents()).isEmpty();
    assertThat(entry.countryContinents()).isEmpty();
  }

  @Test
  void givenListsAreKept() {
    WikidataEntry entry =
        new WikidataEntry("Nile", 10, List.of("Nil"), List.of("Africa"), List.of("Asia"));

    assertThat(entry.aliases()).containsExactly("Nil");
    assertThat(entry.continents()).containsExactly("Africa");
    assertThat(entry.countryContinents()).containsExactly("Asia");
  }
}
