package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import com.parkinson.benjamin.geogames.service.RiverLoaderService.RiverMetadata;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.core.type.TypeReference;

class RiverLoaderServiceTest {

  @Test
  void everyRiverFileHasCompleteAdditionalData() {
    // A river file with no entry in additionalData.json fails loadRivers() with an
    // IllegalStateException naming the river, so this test doubles as a data integrity guard.
    List<River> rivers =
        new RiverLoaderService(new JsonResourceReader(new PathMatchingResourcePatternResolver()))
            .loadRivers();

    assertThat(rivers).isNotEmpty();
    assertThat(rivers.stream().map(River::name).toList()).doesNotHaveDuplicates();
    for (River river : rivers) {
      assertThat(river.name()).as("name").isNotBlank();
      assertThat(river.additionalNames()).as("additionalNames of " + river.name()).isNotNull();
      // A blank alias would match any guess that normalises to nothing, such as "!!!".
      assertThat(river.additionalNames())
          .as("additionalNames of " + river.name())
          .allSatisfy(alias -> assertThat(alias).isNotBlank());
      assertThat(river.continent()).as("continent of " + river.name()).isNotBlank();
      assertThat(river.geoData().geometry().type())
          .as("geometry of " + river.name())
          .isEqualTo("MultiLineString");
    }
  }

  // ----------------------------------------------------- with a stubbed reader

  private static GeoData geoData(String name) {
    return new GeoData(
        "Feature", new GeoProperties(name), new GeoGeometry("MultiLineString", List.of()));
  }

  private RiverLoaderService serviceWith(
      Map<String, RiverMetadata> metadata, GeoData... riverFiles) {
    JsonResourceReader reader = mock(JsonResourceReader.class);
    when(reader.read(eq("data/rivers/additionalData.json"), any(TypeReference.class)))
        .thenReturn(metadata);
    when(reader.readAll(anyString(), eq(GeoData.class))).thenReturn(List.of(riverFiles));
    return new RiverLoaderService(reader);
  }

  @Test
  void combinesEachRiverFileWithItsMetadata() {
    GeoData nile = geoData("Nile");
    RiverLoaderService service =
        serviceWith(Map.of("Nile", new RiverMetadata(List.of("Nil"), "Africa")), nile);

    assertThat(service.loadRivers())
        .containsExactly(new River("Nile", List.of("Nil"), "Africa", nile));
  }

  @Test
  void aRiverWithNoMetadataEntryFailsNamingTheRiver() {
    RiverLoaderService service =
        serviceWith(
            Map.of("Nile", new RiverMetadata(List.of(), "Africa")),
            geoData("Nile"),
            geoData("Lethe"));

    assertThatThrownBy(service::loadRivers)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Lethe")
        .hasMessageContaining("additionalData.json");
  }

  @Test
  void missingAliasesBecomeAnEmptyListNotNull() {
    RiverLoaderService service =
        serviceWith(Map.of("Nile", new RiverMetadata(null, "Africa")), geoData("Nile"));

    assertThat(service.loadRivers())
        .singleElement()
        .satisfies(r -> assertThat(r.additionalNames()).isEmpty());
  }
}
