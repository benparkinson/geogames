package com.parkinson.benjamin.geogames.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.RiverRound;
import com.parkinson.benjamin.geogames.service.CountryShapeService;
import com.parkinson.benjamin.geogames.service.RiverLoaderService;
import com.parkinson.benjamin.geogames.service.RoundGenerator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RiverGamesConfigTest {

  private static final Coordinate POINT = new Coordinate(1, 1);

  private final RiverLoaderService rivers = mock(RiverLoaderService.class);
  private final CountryShapeService countries = mock(CountryShapeService.class);
  private final Random random = new Random(7);
  private final RiverGamesConfig config = new RiverGamesConfig();

  private static River river(String id, Coordinate source, Coordinate mouth, List<String> where) {
    return new River(id, "River " + id, List.of(), "Asia", null, source, mouth, where);
  }

  @BeforeEach
  void setUp() {
    when(rivers.loadRivers())
        .thenReturn(
            List.of(
                river("A", POINT, POINT, List.of("X")),
                river("B", POINT, POINT, List.of("X", "Y", "Z")),
                river("C", null, null, List.of("X", "Y", "Z")),
                river("D", POINT, POINT, null),
                river("E", POINT, null, List.of("X", "Y"))));
    when(countries.loadDisplayCountry(anyString()))
        .thenAnswer(call -> new Country(call.getArgument(0), List.of(), null));
  }

  private static List<String> eligibleIds(RoundGenerator generator, int expected) {
    List<String> ids =
        generator.generate(expected, Set.of()).stream()
            .map(round -> ((RiverRound) round).river().id())
            .toList();
    // Asking for one more than is eligible must fail, so the list above is the whole set.
    assertThatThrownBy(() -> generator.generate(expected + 1, Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
    return ids;
  }

  @Test
  void riversByShapeAcceptsEveryRiver() {
    RoundGenerator generator = config.riversByShape(rivers, random);

    assertThat(generator.gameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
    assertThat(eligibleIds(generator, 5)).containsExactlyInAnyOrder("A", "B", "C", "D", "E");
  }

  @Test
  void riversOnMapAcceptsEveryRiver() {
    RoundGenerator generator = config.riversOnMap(rivers, random);

    assertThat(generator.gameType()).isSameAs(GameType.RIVERS_ON_MAP);
    assertThat(eligibleIds(generator, 5)).containsExactlyInAnyOrder("A", "B", "C", "D", "E");
  }

  @Test
  void riversByEndsNeedsBothASourceAndAMouth() {
    RoundGenerator generator = config.riversByEnds(rivers, random);

    assertThat(generator.gameType()).isSameAs(GameType.RIVERS_BY_ENDS);
    assertThat(eligibleIds(generator, 3)).containsExactlyInAnyOrder("A", "B", "D");
  }

  @Test
  void riversByCountriesNeedsThreeOrMoreCountriesAndAttachesThem() {
    RoundGenerator generator = config.riversByCountries(rivers, countries, random);

    assertThat(generator.gameType()).isSameAs(GameType.RIVERS_BY_COUNTRIES);
    assertThat(eligibleIds(generator, 2)).containsExactlyInAnyOrder("B", "C");
    assertThat(generator.generate(2, Set.of()))
        .allSatisfy(
            round ->
                assertThat(((RiverRound) round).countries())
                    .extracting(Country::name)
                    .containsExactly("X", "Y", "Z"));
  }

  @Test
  void theOtherRiverGamesAttachNoCountries() {
    assertThat(config.riversByShape(rivers, random).generate(5, Set.of()))
        .allSatisfy(round -> assertThat(((RiverRound) round).countries()).isEmpty());
    assertThat(config.riversByEnds(rivers, random).generate(3, Set.of()))
        .allSatisfy(round -> assertThat(((RiverRound) round).countries()).isEmpty());
  }
}
