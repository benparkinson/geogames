package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.CountryOutline;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShape.Neighbour;
import com.parkinson.benjamin.geogames.model.CountryShapeRound;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CountryShapeRoundGeneratorTest {

  private final CountryShapeService shapes = mock(CountryShapeService.class);

  private static GeoData data(String name, String type) {
    return new GeoData("Feature", new GeoProperties(name), new GeoGeometry(type, List.of()));
  }

  private static CountryShape shape(String name, double coastKm, List<Neighbour> neighbours) {
    return new CountryShape(
        name,
        List.of(),
        coastKm,
        100,
        neighbours,
        new Bounds(0, 0, 1, 1),
        data(name, "Polygon"),
        data(name, "MultiLineString"),
        data(name, "MultiLineString"));
  }

  private final CountryShape a =
      shape(
          "A",
          500,
          List.of(
              new Neighbour("B", 50, true),
              new Neighbour("C", 40, false),
              new Neighbour("D", 30, true)));
  private final CountryShape b = shape("B", 500, List.of(new Neighbour("A", 50, true)));
  private final CountryShape c = shape("C", 0, List.of());
  private final CountryShape d = shape("D", 500, List.of());

  @BeforeEach
  void setUp() {
    when(shapes.loadShapes()).thenReturn(List.of(a, b, c, d));
    for (CountryShape shape : List.of(a, b, c, d)) {
      when(shapes.loadShape(shape.name())).thenReturn(shape);
    }
  }

  private CountryShapeRoundGenerator generator(
      Predicate<CountryShape> eligible, boolean withNeighbours) {
    return new CountryShapeRoundGenerator(
        GameType.COUNTRIES_BY_NEIGHBOURS, shapes, new Random(3), eligible, withNeighbours);
  }

  @Test
  void generatesRoundsOfTheGivenType() {
    CountryShapeRoundGenerator generator = generator(s -> true, false);

    assertThat(generator.gameType()).isSameAs(GameType.COUNTRIES_BY_NEIGHBOURS);
    assertThat(generator.generate(1, Set.of()))
        .singleElement()
        .isInstanceOfSatisfying(
            CountryShapeRound.class,
            round -> assertThat(round.gameType()).isSameAs(GameType.COUNTRIES_BY_NEIGHBOURS));
  }

  @Test
  void onlyPicksEligibleCountries() {
    List<GameData> rounds = generator(s -> s.coastKm() > 0, false).generate(3, Set.of());

    assertThat(rounds)
        .extracting(r -> ((CountryShapeRound) r).country().name())
        .containsExactlyInAnyOrder("A", "B", "D");
  }

  @Test
  void skipsExcludedSubjects() {
    List<GameData> rounds = generator(s -> true, false).generate(3, Set.of("country:A"));

    assertThat(rounds).extracting(GameData::subjectId).doesNotContain("country:A").hasSize(3);
  }

  @Test
  void attachesOnlyMainlandNeighboursInOrderWhenAsked() {
    CountryShapeRound round =
        (CountryShapeRound) generator(s -> s.name().equals("A"), true).generate(1, Set.of()).get(0);

    assertThat(round.neighbours()).extracting(CountryOutline::name).containsExactly("B", "D");
    assertThat(round.neighbours().get(0).outline()).isSameAs(b.outline());
  }

  @Test
  void attachesNoNeighboursOtherwise() {
    CountryShapeRound round =
        (CountryShapeRound)
            generator(s -> s.name().equals("A"), false).generate(1, Set.of()).get(0);

    assertThat(round.neighbours()).isEmpty();
  }

  @Test
  void usesTheCountrySubjectId() {
    assertThat(generator(s -> true, false).generate(1, Set.of()).get(0).subjectId())
        .startsWith("country:");
  }

  @Test
  void tooFewEligibleCountriesIsAnError() {
    assertThatThrownBy(() -> generator(s -> s.coastKm() == 0, false).generate(2, Set.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("countries");
  }
}
