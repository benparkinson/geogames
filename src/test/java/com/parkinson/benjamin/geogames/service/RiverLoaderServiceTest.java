package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.loaders.rivers.AliasBuilder;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class RiverLoaderServiceTest {

  private static final Set<String> CONTINENTS =
      Set.of("Africa", "Asia", "Europe", "North America", "South America", "Australia");

  // The hand-picked rivers that the continent quotas keep (not the Yenisei).
  private static final List<String> ORIGINAL_RIVERS =
      List.of(
          "Amazon",
          "Congo",
          "Danube",
          "Dnieper",
          "Ganges",
          "Indus",
          "Mekong",
          "Mississippi",
          "Murray River",
          "Niger",
          "Nile",
          "Rhine",
          "Rhone",
          "Rio Grande",
          "Volga",
          "Yangtze",
          "Yellow River",
          "Yukon River",
          "Zambezi");

  @Test
  @SuppressWarnings("unchecked")
  void theGeneratedRiverFilesAreComplete() {
    List<River> rivers =
        new RiverLoaderService(new JsonResourceReader(new PathMatchingResourcePatternResolver()))
            .loadRivers();

    assertThat(rivers).hasSize(50);
    assertThat(rivers.stream().map(River::id).toList()).doesNotHaveDuplicates();
    assertThat(rivers.stream().map(River::name).toList()).containsAll(ORIGINAL_RIVERS);
    for (River river : rivers) {
      String who = river.name() + " (" + river.id() + ")";
      assertThat(river.id()).as("id of " + who).isNotBlank();
      assertThat(river.name()).as("name of " + who).isNotBlank();
      assertThat(river.continent()).as("continent of " + who).isIn(CONTINENTS);
      assertThat(river.additionalNames()).as("additionalNames of " + who).isNotNull();
      // An alias that normalises to "" would match any guess made only of punctuation.
      assertThat(river.additionalNames())
          .as("additionalNames of " + who)
          .allSatisfy(
              alias -> {
                assertThat(alias).isNotBlank();
                assertThat(AliasBuilder.normalise(alias)).isNotEmpty();
              });

      GeoGeometry geometry = river.geoData().geometry();
      assertThat(geometry.type()).as("geometry of " + who).isEqualTo("MultiLineString");
      List<List<List<Number>>> lines = (List<List<List<Number>>>) geometry.coordinates();
      assertThat(lines).as("lines of " + who).isNotEmpty();
      assertThat(lines)
          .as("lines of " + who)
          .allSatisfy(l -> assertThat(l).hasSizeGreaterThanOrEqualTo(2));
      assertThat(lines.stream().mapToInt(List::size).sum())
          .as("points of " + who)
          .isLessThanOrEqualTo(600);
    }
  }

  @Test
  void loadRiversReturnsWhatTheReaderReads() {
    GeoData geoData =
        new GeoData(
            "Feature", new GeoProperties("Nile"), new GeoGeometry("MultiLineString", List.of()));
    River nile =
        new River(
            "Q3392",
            "Nile",
            List.of("Nil"),
            "Africa",
            geoData,
            new Coordinate(-2.0, 30.0),
            new Coordinate(31.0, 31.5),
            List.of("Egypt", "Sudan"));
    JsonResourceReader reader = mock(JsonResourceReader.class);
    when(reader.readAll(anyString(), eq(River.class))).thenReturn(List.of(nile));

    assertThat(new RiverLoaderService(reader).loadRivers()).containsExactly(nile);
  }
}
