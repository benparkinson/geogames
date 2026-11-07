package com.parkinson.benjamin.geogames.loaders.countries;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProcessedCountryReaderTest {

  @TempDir Path directory;

  private static String countryJson(String name, String extra) {
    return """
    {"name":"%s","additionalNames":["x"],%s
     "geoData":{"type":"Feature","properties":{"name":"%s"},
                "geometry":{"type":"Polygon","coordinates":[[[0.5,1.5]]]}}}
    """
        .formatted(name, extra, name);
  }

  @Test
  void readsOnlyJsonFilesSortedByFileName() throws IOException {
    Files.writeString(directory.resolve("b.json"), countryJson("Bravo", ""));
    Files.writeString(directory.resolve("a.json"), countryJson("Alpha", ""));
    Files.writeString(directory.resolve("notes.txt"), "not json at all");

    List<Country> countries = new ProcessedCountryReader().readCountries(directory);

    assertThat(countries.stream().map(Country::name).toList()).containsExactly("Alpha", "Bravo");
    assertThat(countries.get(0).additionalNames()).containsExactly("x");
  }

  @Test
  void toleratesUnknownProperties() throws IOException {
    Files.writeString(directory.resolve("a.json"), countryJson("Alpha", "\"unexpected\":1,"));

    assertThat(new ProcessedCountryReader().readCountries(directory))
        .extracting(Country::name)
        .containsExactly("Alpha");
  }

  @Test
  void emptyDirectoryGivesNoCountries() throws IOException {
    assertThat(new ProcessedCountryReader().readCountries(directory)).isEmpty();
  }
}
