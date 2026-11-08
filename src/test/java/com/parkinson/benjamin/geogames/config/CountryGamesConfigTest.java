package com.parkinson.benjamin.geogames.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShape.Neighbour;
import com.parkinson.benjamin.geogames.model.CountryShapeRound;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import com.parkinson.benjamin.geogames.service.CountryShapeService;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CountryGamesConfigTest {

  private final CountryShapeService shapes = mock(CountryShapeService.class);
  private final Random random = new Random(7);
  private final CountryGamesConfig config = new CountryGamesConfig();

  private static GeoData data(String name, String type) {
    return new GeoData("Feature", new GeoProperties(name), new GeoGeometry(type, List.of()));
  }

  private static CountryShape shape(
      String name, double coastKm, double landKm, List<Neighbour> neighbours) {
    return new CountryShape(
        name,
        List.of(),
        coastKm,
        landKm,
        neighbours,
        new Bounds(0, 0, 1, 1),
        data(name, "Polygon"),
        data(name, "MultiLineString"),
        data(name, "MultiLineString"));
  }

  private static final List<Neighbour> TWO_MAINLAND =
      List.of(new Neighbour("Other", 100, true), new Neighbour("Third", 100, true));

  // Long coast, long border, two mainland neighbours: eligible for all three games.
  private final CountryShape all = shape("All", 500, 500, TWO_MAINLAND);
  // Island: no land border, so neither border game; neighbours only count when mainland.
  private final CountryShape island = shape("Island", 900, 0, List.of());
  // Landlocked: no coast, so not the coastline or land-border games.
  private final CountryShape landlocked = shape("Landlocked", 0, 900, TWO_MAINLAND);
  // Short coast and short border.
  private final CountryShape tiny =
      shape("Tiny", 100, 40, List.of(new Neighbour("Other", 40, true)));
  // Only an outlying neighbour: no mainland neighbours at all.
  private final CountryShape outlying =
      shape("Outlying", 500, 500, List.of(new Neighbour("Other", 500, false)));

  @BeforeEach
  void setUp() {
    List<CountryShape> everything = List.of(all, island, landlocked, tiny, outlying);
    when(shapes.loadShapes()).thenReturn(everything);
    for (CountryShape shape : everything) {
      when(shapes.loadShape(shape.name())).thenReturn(shape);
    }
    for (String name : List.of("Other", "Third")) {
      when(shapes.loadShape(name)).thenReturn(shape(name, 1, 1, List.of()));
    }
  }

  private static List<String> eligibleNames(RoundGenerator generator, int expected) {
    List<String> names =
        generator.generate(expected, Set.of()).stream()
            .map(round -> ((CountryShapeRound) round).country().name())
            .toList();
    assertThatThrownBy(() -> generator.generate(expected + 1, Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
    return names;
  }

  @Test
  void countriesByNeighboursNeedsTwoMainlandNeighboursAndDrawsThem() {
    RoundGenerator generator = config.countriesByNeighbours(shapes, random);

    assertThat(generator.gameType()).isSameAs(GameType.COUNTRIES_BY_NEIGHBOURS);
    assertThat(eligibleNames(generator, 2)).containsExactlyInAnyOrder("All", "Landlocked");
    assertThat(generator.generate(2, Set.of()))
        .allSatisfy(round -> assertThat(((CountryShapeRound) round).neighbours()).hasSize(2));
  }

  @Test
  void countriesByCoastlineNeedsALongCoastAndSomeLandBorder() {
    RoundGenerator generator = config.countriesByCoastline(shapes, random);

    assertThat(generator.gameType()).isSameAs(GameType.COUNTRIES_BY_COASTLINE);
    assertThat(eligibleNames(generator, 2)).containsExactlyInAnyOrder("All", "Outlying");
    assertThat(generator.generate(2, Set.of()))
        .allSatisfy(round -> assertThat(((CountryShapeRound) round).neighbours()).isEmpty());
  }

  @Test
  void countriesByLandBordersNeedsALongBorderACoastAndAMainlandNeighbour() {
    RoundGenerator generator = config.countriesByLandBorders(shapes, random);

    assertThat(generator.gameType()).isSameAs(GameType.COUNTRIES_BY_LAND_BORDERS);
    assertThat(eligibleNames(generator, 1)).containsExactly("All");
    assertThat(generator.generate(1, Set.of()))
        .allSatisfy(round -> assertThat(((CountryShapeRound) round).neighbours()).hasSize(2));
  }
}
