package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.RiverRound;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RiverRoundGeneratorTest {

  private final List<River> rivers =
      IntStream.range(0, 6)
          .mapToObj(
              i -> new River("Q" + i, "River" + i, List.of(), "Asia", null, null, null, List.of()))
          .toList();
  private final RiverLoaderService loader = mock(RiverLoaderService.class);

  @BeforeEach
  void setUp() {
    when(loader.loadRivers()).thenReturn(rivers);
  }

  private RiverRoundGenerator generator(
      Predicate<River> eligible, Function<River, List<Country>> countries) {
    return new RiverRoundGenerator(
        GameType.RIVERS_BY_SHAPE, loader, new Random(3), eligible, countries);
  }

  private RiverRoundGenerator generator() {
    return generator(river -> true, river -> List.of());
  }

  @Test
  void generatesRoundsOfTheGivenType() {
    RiverRoundGenerator onMap =
        new RiverRoundGenerator(
            GameType.RIVERS_BY_SHAPE, loader, new Random(3), r -> true, r -> List.of());

    assertThat(onMap.gameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
    assertThat(onMap.generate(1, Set.of()))
        .singleElement()
        .isInstanceOfSatisfying(
            RiverRound.class,
            round -> assertThat(round.gameType()).isSameAs(GameType.RIVERS_BY_SHAPE));
  }

  @Test
  void returnsDistinctRiversFromTheInput() {
    List<GameData> result = generator().generate(3, Set.of());

    assertThat(result).hasSize(3).doesNotHaveDuplicates();
    assertThat(result.stream().map(d -> ((RiverRound) d).river())).isSubsetOf(rivers);
  }

  @Test
  void returnsEveryRiverWhenAskedForAll() {
    assertThat(generator().generate(rivers.size(), Set.of()))
        .extracting(GameData::subjectId)
        .containsExactlyInAnyOrderElementsOf(rivers.stream().map(r -> "river:" + r.id()).toList());
  }

  @Test
  void returnsNothingWhenAskedForZero() {
    assertThat(generator().generate(0, Set.of())).isEmpty();
  }

  @Test
  void rejectsMoreRiversThanAreKnown() {
    assertThatThrownBy(() -> generator().generate(7, Set.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("7")
        .hasMessageContaining("6");
  }

  @Test
  void onlyPicksEligibleRivers() {
    RiverRoundGenerator evens =
        generator(river -> Integer.parseInt(river.id().substring(1)) % 2 == 0, r -> List.of());

    assertThat(evens.generate(3, Set.of()))
        .extracting(GameData::subjectId)
        .containsExactlyInAnyOrder("river:Q0", "river:Q2", "river:Q4");
    assertThatThrownBy(() -> evens.generate(4, Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void skipsExcludedSubjects() {
    Set<String> excluded = Set.of("river:Q0", "river:Q1", "river:Q2");

    assertThat(generator().generate(3, excluded))
        .extracting(GameData::subjectId)
        .containsExactlyInAnyOrder("river:Q3", "river:Q4", "river:Q5");
    assertThatThrownBy(() -> generator().generate(4, excluded))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void attachesTheCountriesInTheOrderGiven() {
    Country france = new Country("France", List.of(), null);
    Country spain = new Country("Spain", List.of(), null);
    RiverRoundGenerator withCountries = generator(river -> true, river -> List.of(spain, france));

    RiverRound round = (RiverRound) withCountries.generate(1, Set.of()).get(0);

    assertThat(round.countries()).containsExactly(spain, france);
  }
}
