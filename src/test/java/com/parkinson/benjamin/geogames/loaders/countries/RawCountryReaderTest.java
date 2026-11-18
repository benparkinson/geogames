package com.parkinson.benjamin.geogames.loaders.countries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RawCountryReaderTest {

  @TempDir Path directory;

  private List<Country> read(Path countries, Path aliases, Path excluded) throws IOException {
    return readWithOfficialNames("{}", countries, aliases, excluded);
  }

  private List<Country> readWithOfficialNames(
      String officialNames, Path countries, Path aliases, Path excluded) throws IOException {
    Path official = directory.resolve("officialNames.json");
    Files.writeString(official, officialNames);
    return new RawCountryReader().readCountries(countries, official, aliases, excluded);
  }

  private Path write(String fileName, String content) throws IOException {
    return Files.writeString(directory.resolve(fileName), content);
  }

  @Test
  void anOfficialNameReplacesTheSourceNameWhichBecomesAnAlias() throws IOException {
    Path countries = writeTwoCountries();
    Path aliases = write("additionalNames.json", "{\"Alfa\":[\"A-land\"]}");
    Path excluded = write("excludedNames.json", "[]");

    Country renamed =
        readWithOfficialNames("{\"Alpha\":\"Alfa\"}", countries, aliases, excluded).getFirst();

    assertThat(renamed.name()).isEqualTo("Alfa");
    assertThat(renamed.additionalNames()).containsExactly("Alpha", "A-land");
    assertThat(renamed.geoData().properties().name()).isEqualTo("Alfa");
  }

  @Test
  void aNewSpellingOfTheSameNameAddsNoAlias() throws IOException {
    Path countries = writeTwoCountries();
    Path aliases = write("additionalNames.json", "{}");
    Path excluded = write("excludedNames.json", "[]");

    Country renamed =
        readWithOfficialNames("{\"Alpha\":\"Álpha\"}", countries, aliases, excluded).getFirst();

    assertThat(renamed.name()).isEqualTo("Álpha");
    assertThat(renamed.additionalNames()).isEmpty();
  }

  @Test
  void anOfficialNameForAnUnknownFeatureThrows() throws IOException {
    Path countries = writeTwoCountries();
    Path aliases = write("additionalNames.json", "{}");
    Path excluded = write("excludedNames.json", "[]");

    assertThatThrownBy(
            () -> readWithOfficialNames("{\"Typo\":\"Alfa\"}", countries, aliases, excluded))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Typo");
  }

  @Test
  void aliasesForAnUnknownCountryThrow() throws IOException {
    Path countries = writeTwoCountries();
    Path aliases = write("additionalNames.json", "{\"Typo\":[\"T\"]}");
    Path excluded = write("excludedNames.json", "[]");

    assertThatThrownBy(() -> read(countries, aliases, excluded))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Typo");
  }

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
    Path excluded = directory.resolve("excludedNames.json");
    Files.writeString(excluded, "[]");

    List<Country> result = read(countries, aliases, excluded);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).name()).isEqualTo("Alpha");
    assertThat(result.get(0).additionalNames()).containsExactly("Alp", "A-land");
    assertThat(result.get(0).geoData().geometry().type()).isEqualTo("Polygon");
    assertThat(result.get(1).name()).isEqualTo("Bravo");
    assertThat(result.get(1).additionalNames()).isEmpty();
    assertThat(result.get(1).geoData().geometry().type()).isEqualTo("MultiPolygon");
  }

  private Path writeTwoCountries() throws IOException {
    Path countries = directory.resolve("countries.geo.json");
    Files.writeString(
        countries,
        """
        [
          {"type":"Feature","properties":{"name":"Alpha"},
           "geometry":{"type":"Polygon","coordinates":[[[0,0],[1,1]]]}},
          {"type":"Feature","properties":{"name":"Bravo"},
           "geometry":{"type":"Polygon","coordinates":[[[0,0],[1,1]]]}}
        ]
        """);
    return countries;
  }

  @Test
  void dropsExcludedNamesAndKeepsTheOthers() throws IOException {
    Path countries = writeTwoCountries();
    Path aliases = directory.resolve("additionalNames.json");
    Files.writeString(aliases, "{}");
    Path excluded = directory.resolve("excludedNames.json");
    Files.writeString(excluded, "[\"Alpha\"]");

    List<Country> result = read(countries, aliases, excluded);

    assertThat(result).extracting(Country::name).containsExactly("Bravo");
  }

  @Test
  void excludedNameMatchingNoFeatureThrows() throws IOException {
    Path countries = writeTwoCountries();
    Path aliases = directory.resolve("additionalNames.json");
    Files.writeString(aliases, "{}");
    Path excluded = directory.resolve("excludedNames.json");
    Files.writeString(excluded, "[\"Alpha\",\"Typo\"]");

    assertThatThrownBy(() -> read(countries, aliases, excluded))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Typo")
        .hasMessageNotContaining("Alpha,");
  }
}
