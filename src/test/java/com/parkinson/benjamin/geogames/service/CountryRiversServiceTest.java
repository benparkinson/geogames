package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CountryRiversServiceTest {

  private final JsonResourceReader reader = mock(JsonResourceReader.class);
  private final CountryRiversService service = new CountryRiversService(reader);

  private static GeoData lines(String name) {
    return new GeoData(
        "Feature", new GeoProperties(name), new GeoGeometry("MultiLineString", List.of()));
  }

  private static CountryRivers rivers(String name) {
    return new CountryRivers(
        name,
        lines(name),
        lines(name),
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry("MultiPolygon", List.of())),
        null);
  }

  @BeforeEach
  void setUp() {
    when(reader.read("processed/country-rivers/index.json", CountryRiversIndexEntry[].class))
        .thenReturn(
            new CountryRiversIndexEntry[] {
              new CountryRiversIndexEntry("A", "A.json", 5, 1_000, 1),
              new CountryRiversIndexEntry("United States", "UnitedStates.json", 9, 9_000, 4)
            });
    when(reader.read("processed/country-rivers/A.json", CountryRivers.class))
        .thenReturn(rivers("A"));
    when(reader.read("processed/country-rivers/UnitedStates.json", CountryRivers.class))
        .thenReturn(rivers("United States"));
  }

  @Test
  void theIndexIsReadOnceAndKept() {
    assertThat(service.index())
        .extracting(CountryRiversIndexEntry::name)
        .containsExactly("A", "United States");
    assertThat(service.find("United States").riverCount()).isEqualTo(9);
    assertThat(service.find("Nowhere")).isNull();

    verify(reader, times(1))
        .read("processed/country-rivers/index.json", CountryRiversIndexEntry[].class);
  }

  @Test
  void loadReadsTheCountrysFileByTheNameInTheIndex() {
    assertThat(service.load("United States").name()).isEqualTo("United States");

    verify(reader).read("processed/country-rivers/UnitedStates.json", CountryRivers.class);
  }

  @Test
  void loadReadsTheFileEveryTimeAndKeepsNothing() {
    service.load("A");
    service.load("A");

    verify(reader, times(2)).read("processed/country-rivers/A.json", CountryRivers.class);
  }

  @Test
  void loadingAnUnknownCountryReadsNoFileAndIsAnError() {
    assertThatThrownBy(() -> service.load("Nowhere"))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("Nowhere");

    verify(reader, never()).read("processed/country-rivers/Nowhere.json", CountryRivers.class);
  }
}
