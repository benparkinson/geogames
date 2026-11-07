package com.parkinson.benjamin.geogames.loaders.countries;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CountryWriterTest {

  @TempDir Path directory;

  private static Country country(String name, List<String> aliases) {
    List<List<List<Double>>> ring =
        List.of(List.of(List.of(0.5, 0.5), List.of(1.5, 0.5), List.of(1.5, 1.5)));
    GeoData geoData =
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry("Polygon", ring));
    return new Country(name, aliases, geoData);
  }

  @Test
  void writesOneFilePerCountryNamedByTheCountryHelper() throws IOException {
    new CountryWriter()
        .writeCountries(
            List.of(country("Costa Rica", List.of()), country("St. Lucia", List.of())), directory);

    try (var files = Files.list(directory)) {
      assertThat(files.map(p -> p.getFileName().toString()).toList())
          .containsExactlyInAnyOrder("CostaRica.json", "StLucia.json");
    }
  }

  @Test
  void clearOutputDeletesJsonFilesOnly() throws IOException {
    Files.writeString(directory.resolve("Old.json"), "{}");
    Files.writeString(directory.resolve("notes.txt"), "keep");
    Path sub = Files.createDirectory(directory.resolve("sub"));
    Files.writeString(sub.resolve("Nested.json"), "{}");

    new CountryWriter().clearOutput(directory);

    assertThat(directory.resolve("Old.json")).doesNotExist();
    assertThat(directory.resolve("notes.txt")).exists();
    assertThat(sub.resolve("Nested.json")).exists();
  }

  @Test
  void outputRoundTripsThroughTheProcessedReader() throws IOException {
    List<Country> countries =
        List.of(country("Alpha", List.of("A")), country("Beta", List.of("B", "Bee")));

    new CountryWriter().writeCountries(countries, directory);

    assertThat(new ProcessedCountryReader().readCountries(directory))
        .containsExactlyInAnyOrderElementsOf(countries);
  }

  @Test
  void propertiesAreWrittenInDeclarationOrder() throws IOException {
    new CountryWriter().writeCountries(List.of(country("Alpha", List.of("A"))), directory);

    String json = Files.readString(directory.resolve("Alpha.json"));

    assertThat(json.indexOf("\"name\""))
        .isLessThan(json.indexOf("\"additionalNames\""))
        .isGreaterThanOrEqualTo(0);
    assertThat(json.indexOf("\"additionalNames\"")).isLessThan(json.indexOf("\"geoData\""));
  }
}
