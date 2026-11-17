package com.parkinson.benjamin.geogames.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.CountryPairRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShape.Neighbour;
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

class CountryPairGamesConfigTest {

  private final CountryShapeService shapes = mock(CountryShapeService.class);
  private final Random random = new Random(7);
  private final CountryPairGamesConfig config = new CountryPairGamesConfig();

  private static GeoData data(String type, List<?> coordinates) {
    return new GeoData("Feature", new GeoProperties("x"), new GeoGeometry(type, coordinates));
  }

  private static CountryShape shape(String name, long areaKm2, Neighbour... neighbours) {
    List<?> border = List.of(List.of(List.of(0.0, 0.0), List.of(1.0, 1.0)));
    return new CountryShape(
        name,
        List.of(),
        100,
        100,
        areaKm2,
        List.of(neighbours),
        new Bounds(0, 0, 1, 1),
        data("Polygon", List.of()),
        data("MultiLineString", List.of()),
        data("MultiLineString", border));
  }

  private static Neighbour neighbour(String name, double km) {
    return new Neighbour(name, km, true, List.of(0));
  }

  // A and B are alike in size and share a long border; A and C share one too, but C is huge.
  private final CountryShape a =
      shape("A", 1000, neighbour("B", 100), neighbour("C", 100), neighbour("D", 10));
  private final CountryShape b = shape("B", 2000, neighbour("A", 100));
  private final CountryShape c = shape("C", 100_000, neighbour("A", 100));
  private final CountryShape d = shape("D", 1000, neighbour("A", 10));

  @BeforeEach
  void setUp() {
    List<CountryShape> all = List.of(a, b, c, d);
    when(shapes.loadShapes()).thenReturn(all);
    for (CountryShape shape : all) {
      when(shapes.loadShape(shape.name())).thenReturn(shape);
    }
  }

  private static List<String> subjects(RoundGenerator generator, int expected) {
    List<String> subjects =
        generator.generate(expected, Set.of(), new Random(7)).stream()
            .map(r -> r.subjectId())
            .toList();
    assertThatThrownBy(() -> generator.generate(expected + 1, Set.of(), new Random(7)))
        .isInstanceOf(IllegalArgumentException.class);
    return subjects;
  }

  @Test
  void bordersByShapeNeedsALongBorderAndShowsNoNeighbours() {
    RoundGenerator generator = config.bordersByShape(shapes);

    assertThat(generator.gameType()).isSameAs(GameType.BORDERS_BY_SHAPE);
    assertThat(subjects(generator, 2)).containsExactlyInAnyOrder("pair:A|B", "pair:A|C");
    assertThat(generator.generate(2, Set.of(), random))
        .allSatisfy(round -> assertThat(((CountryPairRound) round).neighbours()).isEmpty());
  }

  @Test
  void mergedCountriesAlsoNeedSimilarSizesAndShowTheSurroundings() {
    RoundGenerator generator = config.mergedCountries(shapes);

    assertThat(generator.gameType()).isSameAs(GameType.MERGED_COUNTRIES);
    assertThat(subjects(generator, 1)).containsExactly("pair:A|B");
    assertThat(generator.generate(1, Set.of(), random))
        .singleElement()
        .satisfies(round -> assertThat(((CountryPairRound) round).neighbours()).hasSize(2));
  }
}
