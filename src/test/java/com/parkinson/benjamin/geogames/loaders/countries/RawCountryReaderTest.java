package com.parkinson.benjamin.geogames.loaders.countries;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RawCountryReaderTest {

  @TempDir Path directory;

  @Test
  void attachesAliasesAndDefaultsToAnEmptyList() throws IOException {
    Path countries = directory.resolve("countries.geo.json");
    Path aliases = directory.resolve("additionalNames.json");
    Files.writeString(
        countries,
        """
        [
          {"type":"Feature","properties":{"name":"Alpha","ignored":"yes"},
           "geometry":{"type":"Polygon","coordinates":[[[0,0],[1,1]]]}},
          {"type":"Feature","properties":{"name":"Bravo"},
           "geometry":{"type":"MultiPolygon","coordinates":[[[[0,0],[1,1]]]]}}
        ]
        """);
    Files.writeString(aliases, "{\"Alpha\":[\"Alp\",\"A-land\"]}");

    List<Country> result = new RawCountryReader().readCountries(countries, aliases);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).name()).isEqualTo("Alpha");
    assertThat(result.get(0).additionalNames()).containsExactly("Alp", "A-land");
    assertThat(result.get(0).geoData().geometry().type()).isEqualTo("Polygon");
    assertThat(result.get(1).name()).isEqualTo("Bravo");
    assertThat(result.get(1).additionalNames()).isEmpty();
    assertThat(result.get(1).geoData().geometry().type()).isEqualTo("MultiPolygon");
  }
}
