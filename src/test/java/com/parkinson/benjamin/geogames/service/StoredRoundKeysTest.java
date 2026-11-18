package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StoredRoundKeysTest {

  private static final Map<String, String> RENAMES =
      Map.of("Swaziland", "Eswatini", "Turkey", "Türkiye");

  private static String update(String key) {
    return StoredRoundKeys.withCurrentCountryNames(key, name -> RENAMES.getOrDefault(name, name));
  }

  @ParameterizedTest
  @CsvSource({
    "country:Swaziland, country:Eswatini",
    "country:France, country:France",
    "pair:Mozambique|Swaziland, pair:Eswatini|Mozambique",
    "pair:Greece|Turkey, pair:Greece|Türkiye",
    "'tripoint:Mozambique|South Africa|Swaziland@-25.9,32.0',"
        + " 'tripoint:Eswatini|Mozambique|South Africa@-25.9,32.0'"
  })
  void usesTheCurrentNamesAndKeepsTheKeySorted(String stored, String expected) {
    assertThat(update(stored)).isEqualTo(expected);
  }

  @Test
  void leavesKeysThatNameNoCountryAlone() {
    assertThat(update("river:Q3392")).isEqualTo("river:Q3392");
    assertThat(update("lake:Q5513")).isEqualTo("lake:Q5513");
  }

  @Test
  void leavesAMalformedPairForTheGeneratorToReject() {
    assertThat(update("pair:Swaziland")).isEqualTo("pair:Swaziland");
  }
}
