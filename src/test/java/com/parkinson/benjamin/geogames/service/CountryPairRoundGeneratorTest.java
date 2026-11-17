package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.helper.CountryPairEligibility;
import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.CountryOutline;
import com.parkinson.benjamin.geogames.model.CountryPairRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShape.Neighbour;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CountryPairRoundGeneratorTest {

  private final Random random = new Random(3);

  private static final BiPredicate<CountryShape, CountryShape> BORDERING =
      CountryPairEligibility.borders();

  private final CountryShapeService shapes = mock(CountryShapeService.class);

  private static List<List<Double>> line(double... lonLat) {
    return List.of(List.of(lonLat[0], lonLat[1]), List.of(lonLat[2], lonLat[3]));
  }

  private static GeoData outline(String name) {
    return new GeoData("Feature", new GeoProperties(name), new GeoGeometry("Polygon", List.of()));
  }

  /** Borders the neighbours with one line each, the nth line being for the nth neighbour. */
  private static CountryShape shape(
      String name, Bounds focus, List<Neighbour> neighbours, List<List<List<Double>>> lines) {
    return new CountryShape(
        name,
        List.of(),
        100,
        100,
        1000,
        neighbours,
        focus,
        outline(name),
        new GeoData(
            "Feature", new GeoProperties(name), new GeoGeometry("MultiLineString", List.of())),
        new GeoData("Feature", new GeoProperties(name), new GeoGeometry("MultiLineString", lines)));
  }

  // A and B share the line (5,0)-(6,1), A and C the line (0,0)-(1,1), B and D (9,9)-(10,10).
  private final CountryShape a =
      shape(
          "A",
          new Bounds(0, 0, 6, 1),
          List.of(
              new Neighbour("B", 77, true, List.of(1)), new Neighbour("C", 60, true, List.of(0))),
          List.of(line(0, 0, 1, 1), line(5, 0, 6, 1)));
  private final CountryShape b =
      shape(
          "B",
          new Bounds(5, -2, 10, 10),
          List.of(
              new Neighbour("A", 77, true, List.of(0)), new Neighbour("D", 80, true, List.of(1))),
          List.of(line(5, 0, 6, 1), line(9, 9, 10, 10)));
  private final CountryShape c =
      shape(
          "C",
          new Bounds(-3, 0, 1, 1),
          List.of(new Neighbour("A", 60, true, List.of(0))),
          List.of(line(0, 0, 1, 1)));
  private final CountryShape d =
      shape(
          "D",
          new Bounds(9, 9, 12, 12),
          List.of(new Neighbour("B", 80, true, List.of(0))),
          List.of(line(9, 9, 10, 10)));

  @BeforeEach
  void setUp() {
    List<CountryShape> all = List.of(a, b, c, d);
    when(shapes.loadShapes()).thenReturn(all);
    for (CountryShape shape : all) {
      when(shapes.loadShape(shape.name())).thenReturn(shape);
    }
  }

  private CountryPairRoundGenerator generator(
      BiPredicate<CountryShape, CountryShape> eligible, boolean withNeighbours) {
    return new CountryPairRoundGenerator(
        GameType.MERGED_COUNTRIES, shapes, eligible, withNeighbours);
  }

  private static CountryPairRound pair(CountryPairRoundGenerator generator, String key) {
    return (CountryPairRound) generator.rebuild(key);
  }

  @Test
  void listsEveryEligiblePairOnceWithTheCountriesInNameOrder() {
    List<GameData> rounds = generator(BORDERING, false).generate(3, Set.of(), random);

    // Of the six name pairs only A|B, A|C and B|D border each other.
    assertThat(rounds)
        .extracting(GameData::subjectId)
        .containsExactlyInAnyOrder("pair:A|B", "pair:A|C", "pair:B|D");
    assertThat(rounds)
        .allSatisfy(
            round -> {
              CountryPairRound r = (CountryPairRound) round;
              assertThat(r.gameType()).isSameAs(GameType.MERGED_COUNTRIES);
              assertThat(r.countries().get(0).name()).isLessThan(r.countries().get(1).name());
            });
  }

  @Test
  void onlyPicksEligiblePairsThatAreNotExcluded() {
    List<GameData> rounds =
        generator(BORDERING.and((x, y) -> x.name().equals("A")), false)
            .generate(1, Set.of("pair:A|B"), random);

    assertThat(rounds).extracting(GameData::subjectId).containsExactly("pair:A|C");
  }

  @Test
  void tooFewEligiblePairsIsAnError() {
    assertThatThrownBy(
            () ->
                generator(BORDERING.and((x, y) -> x.name().equals("A")), false)
                    .generate(3, Set.of(), random))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("country pairs");
  }

  @Test
  void theSharedBorderIsTheFirstCountrysLinesForTheSecond() {
    CountryPairRound round = pair(generator(BORDERING, false), "pair:A|B");

    assertThat(round.sharedBorder().geometry().type()).isEqualTo("MultiLineString");
    assertThat(round.sharedBorder().geometry().coordinates()).containsExactly(line(5, 0, 6, 1));
    assertThat(round.borderKm()).isEqualTo(77);
    assertThat(round.borderBounds()).isEqualTo(new Bounds(5, 0, 6, 1));
  }

  @Test
  void theFocusIsTheUnionOfBothFocusBoxes() {
    CountryPairRound round = pair(generator(BORDERING, false), "pair:A|B");

    assertThat(round.focus()).isEqualTo(new Bounds(0, -2, 10, 10));
  }

  @Test
  void attachesTheMainlandNeighboursOfEitherCountryExceptThePairWhenAsked() {
    CountryPairRound round = pair(generator(BORDERING, true), "pair:A|B");

    assertThat(round.neighbours()).extracting(CountryOutline::name).containsExactly("C", "D");
    assertThat(round.neighbours().get(0).outline()).isSameAs(c.outline());
  }

  @Test
  void leavesOutOverseasNeighboursAndCountsEachNeighbourOnce() {
    CountryShape a2 =
        shape(
            "A",
            new Bounds(0, 0, 6, 1),
            List.of(
                new Neighbour("B", 77, true, List.of(0)),
                new Neighbour("C", 60, false, List.of(0)),
                new Neighbour("D", 50, true, List.of(0))),
            List.of(line(5, 0, 6, 1)));
    when(shapes.loadShapes()).thenReturn(List.of(a2, b, c, d));
    when(shapes.loadShape("A")).thenReturn(a2);

    CountryPairRound round = pair(generator(BORDERING, true), "pair:A|B");

    // C is only an overseas neighbour of A, and D borders both A and B.
    assertThat(round.neighbours()).extracting(CountryOutline::name).containsExactly("D");
  }

  @Test
  void attachesNoNeighboursOtherwise() {
    assertThat(pair(generator(BORDERING, false), "pair:A|B").neighbours()).isEmpty();
  }

  @Test
  void rebuildGivesBackEveryGeneratedRoundWithAndWithoutNeighbours() {
    ObjectMapper mapper = JsonMapper.builder().build();
    for (boolean withNeighbours : new boolean[] {true, false}) {
      CountryPairRoundGenerator generator = generator(BORDERING, withNeighbours);

      for (GameData round : generator.generate(3, Set.of(), random)) {
        GameData rebuilt = generator.rebuild(round.roundKey());

        assertThat(rebuilt).isEqualTo(round);
        assertThat(mapper.writeValueAsString(rebuilt)).isEqualTo(mapper.writeValueAsString(round));
      }
    }
  }

  @Test
  void rebuildRejectsUnknownKeysWrongPrefixesAndPairsThatDontBorder() {
    CountryPairRoundGenerator generator = generator(BORDERING, true);

    for (String key :
        List.of(
            "pair:A|Nowhere",
            "pair:Nowhere|B",
            "river:A",
            "country:A",
            "pair:A",
            "pair:A|B|C",
            "pair:B|A",
            "pair:A|D")) {
      assertThatThrownBy(() -> generator.rebuild(key))
          .as(key)
          .isInstanceOf(RoundDataMissingException.class);
    }
  }

  @Test
  void rebuildIgnoresTheEligibilityFilter() {
    CountryPairRoundGenerator none = generator((x, y) -> false, false);

    assertThat(none.rebuild("pair:A|B").subjectId()).isEqualTo("pair:A|B");
    assertThat(none.gameType()).isSameAs(GameType.MERGED_COUNTRIES);
  }
}
