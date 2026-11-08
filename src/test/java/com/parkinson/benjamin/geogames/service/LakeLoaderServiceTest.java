package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.loaders.features.AliasBuilder;
import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.PlaceName;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class LakeLoaderServiceTest {

  private static final Set<String> CONTINENTS =
      Set.of("Africa", "Asia", "Europe", "North America", "South America", "Australia");

  private static Lake lake(String id, String name, List<String> aliases) {
    GeoData geoData =
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry("Polygon", List.of()));
    return new Lake(id, name, aliases, "Asia", geoData, List.of("Russia"));
  }

  @Test
  void theGeneratedLakeFilesAreComplete() {
    List<Lake> lakes =
        new LakeLoaderService(new JsonResourceReader(new PathMatchingResourcePatternResolver()))
            .loadLakes();

    assertThat(lakes).hasSizeGreaterThanOrEqualTo(20);
    assertThat(lakes.stream().map(Lake::id).toList()).doesNotHaveDuplicates();
    for (Lake lake : lakes) {
      String who = lake.name() + " (" + lake.id() + ")";
      assertThat(lake.name()).as("name of " + who).isNotBlank();
      assertThat(lake.continent()).as("continent of " + who).isIn(CONTINENTS);
      assertThat(lake.geoData().geometry().type())
          .as("geometry of " + who)
          .isIn("Polygon", "MultiPolygon");
      assertThat(lake.additionalNames())
          .as("additionalNames of " + who)
          .allSatisfy(alias -> assertThat(AliasBuilder.normalise(alias)).isNotEmpty());
    }
  }

  @Test
  void loadLakesReturnsWhatTheReaderReads() {
    Lake baikal = lake("Q1", "Lake Baikal", List.of("Baikal"));
    JsonResourceReader reader = mock(JsonResourceReader.class);
    when(reader.readAll(anyString(), eq(Lake.class))).thenReturn(List.of(baikal));

    assertThat(new LakeLoaderService(reader).loadLakes()).containsExactly(baikal);
  }

  @Test
  void loadLakeNamesAreSortedAndNullAliasesBecomeEmpty() {
    JsonResourceReader reader = mock(JsonResourceReader.class);
    when(reader.readAll(anyString(), eq(Lake.class)))
        .thenReturn(
            List.of(lake("Q1", "Titicaca", null), lake("Q2", "Lake Baikal", List.of("Baikal"))));

    assertThat(new LakeLoaderService(reader).loadLakeNames())
        .containsExactly(
            new PlaceName("Lake Baikal", List.of("Baikal")), new PlaceName("Titicaca", List.of()));
  }
}
