package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class CountryPairRoundTest {

  private static GeoData data(String type) {
    return new GeoData("Feature", new GeoProperties("x"), new GeoGeometry(type, List.of()));
  }

  private static CountryShape shape(String name) {
    return new CountryShape(
        name,
        List.of(),
        0,
        0,
        0,
        List.of(),
        new Bounds(0, 0, 1, 1),
        data("Polygon"),
        data("MultiLineString"),
        data("MultiLineString"));
  }

  private static CountryPairRound round(GameType type, List<CountryShape> countries) {
    return new CountryPairRound(
        type,
        countries,
        data("MultiLineString"),
        10,
        null,
        new Bounds(0, 0, 1, 1),
        new Bounds(0, 0, 1, 1));
  }

  @Test
  void bothGamesShareTheSubjectIdWhateverTheOrder() {
    CountryPairRound border = round(GameType.BORDERS_BY_SHAPE, List.of(shape("A"), shape("B")));
    CountryPairRound merged = round(GameType.MERGED_COUNTRIES, List.of(shape("A"), shape("B")));

    assertThat(border.subjectId()).isEqualTo("pair:A|B").isEqualTo(merged.subjectId());
    assertThat(border.roundKey()).isEqualTo("pair:A|B");
    assertThat(CountryPairRound.subjectId("B", "A")).isEqualTo("pair:A|B");
  }

  @Test
  void noNeighboursBecomesAnEmptyList() {
    assertThat(round(GameType.BORDERS_BY_SHAPE, List.of(shape("A"), shape("B"))).neighbours())
        .isEmpty();
  }
}
