package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class TripointFinderTest {

  private TripointLoaderService loader;
  private CountryShapeService countryLoader;
  private TripointFinderService service;
  private List<TripointDefinition> definitions;

  @BeforeEach
  void setUp() {
    loader = mock(TripointLoaderService.class);
    countryLoader = mock(CountryShapeService.class);
    service = new TripointFinderService(loader, countryLoader, new Random(5));

    definitions =
        IntStream.range(0, 5)
            .mapToObj(
                i ->
                    new TripointDefinition(
                        new Coordinate(i, i * 2), List.of("A" + i, "B" + i, "C" + i)))
            .toList();
    when(loader.loadTripointDefinitions()).thenReturn(definitions);
    when(countryLoader.loadDisplayCountry(anyString()))
        .thenAnswer(
            invocation -> {
              String name = invocation.getArgument(0);
              return new Country(name, List.of(), null);
            });
  }

  @Test
  public void rejectsRequestForTooManyTripoints() {
    TripointLoaderService emptyLoader = mock(TripointLoaderService.class);
    when(emptyLoader.loadTripointDefinitions()).thenReturn(List.of());
    TripointFinderService emptyService =
        new TripointFinderService(emptyLoader, mock(CountryShapeService.class), new Random(5));

    assertThrows(IllegalArgumentException.class, () -> emptyService.generate(1, Set.of()));
  }

  @Test
  void generatesRoundsOfTheTripointType() {
    assertThat(service.gameType()).isSameAs(GameType.TRIPOINT);
  }

  @Test
  void returnsDistinctTripointsBuiltFromDefinitions() {
    List<GameData> result = service.generate(3, Set.of());

    assertThat(result).hasSize(3);
    List<Coordinate> coordinates = result.stream().map(t -> ((Tripoint) t).coordinate()).toList();
    assertThat(coordinates).doesNotHaveDuplicates();
    for (GameData data : result) {
      Tripoint tripoint = (Tripoint) data;
      TripointDefinition definition =
          definitions.stream()
              .filter(d -> d.coordinate().equals(tripoint.coordinate()))
              .findFirst()
              .orElseThrow();
      assertThat(tripoint.countries().stream().map(Country::name).toList())
          .containsExactlyInAnyOrderElementsOf(definition.countryNames());
      for (String name : definition.countryNames()) {
        verify(countryLoader).loadDisplayCountry(name);
      }
    }
  }

  @Test
  void canReturnEveryDefinition() {
    List<GameData> result = service.generate(definitions.size(), Set.of());

    assertThat(result.stream().map(t -> ((Tripoint) t).coordinate()).toList())
        .containsExactlyInAnyOrderElementsOf(
            definitions.stream().map(TripointDefinition::coordinate).toList());
  }

  @Test
  void returnsNothingWithoutLoadingCountriesWhenAskedForZero() {
    assertThat(service.generate(0, Set.of())).isEmpty();
    verifyNoInteractions(countryLoader);
  }

  @Test
  void propagatesCountryLoadingFailures() {
    when(countryLoader.loadDisplayCountry(anyString()))
        .thenThrow(new UncheckedIOException(new IOException("boom")));

    assertThatThrownBy(() -> service.generate(1, Set.of()))
        .isInstanceOf(UncheckedIOException.class)
        .hasRootCauseMessage("boom");
  }

  @Test
  void rebuildGivesBackEveryGeneratedRound() {
    ObjectMapper mapper = JsonMapper.builder().build();

    for (GameData round : service.generate(definitions.size(), Set.of())) {
      GameData rebuilt = service.rebuild(round.roundKey());

      assertThat(rebuilt).isEqualTo(round);
      assertThat(mapper.writeValueAsString(rebuilt)).isEqualTo(mapper.writeValueAsString(round));
    }
  }

  @Test
  void countriesKeepTheDefinitionsOrder() {
    Tripoint tripoint = (Tripoint) service.rebuild(Tripoint.roundKey(definitions.get(2)));

    assertThat(tripoint.countries()).extracting(Country::name).containsExactly("A2", "B2", "C2");
  }

  @Test
  void rebuildTellsTwoTripointsOfTheSameCountriesApart() {
    List<String> names = List.of("A", "B", "C");
    TripointDefinition first = new TripointDefinition(new Coordinate(1, 2), names);
    TripointDefinition second = new TripointDefinition(new Coordinate(3, 4), names);
    when(loader.loadTripointDefinitions()).thenReturn(List.of(first, second));

    assertThat(((Tripoint) service.rebuild(Tripoint.roundKey(second))).coordinate())
        .isEqualTo(second.coordinate());
    assertThat(((Tripoint) service.rebuild(Tripoint.roundKey(first))).coordinate())
        .isEqualTo(first.coordinate());
  }

  @Test
  void rebuildRejectsUnknownKeysAndWrongPrefixes() {
    assertThatThrownBy(() -> service.rebuild("tripoint:A0|B0|C0@9.0,9.0"))
        .isInstanceOf(RoundDataMissingException.class);
    assertThatThrownBy(() -> service.rebuild("river:Q1"))
        .isInstanceOf(RoundDataMissingException.class);
  }

  @Test
  void roundKeyIncludesTheCoordinate() {
    Tripoint tripoint = (Tripoint) service.generate(1, Set.of()).get(0);

    assertThat(tripoint.roundKey())
        .isEqualTo(
            tripoint.subjectId()
                + "@"
                + tripoint.coordinate().latitude()
                + ","
                + tripoint.coordinate().longitude());
    assertThat(tripoint.roundKey())
        .isEqualTo(
            Tripoint.roundKey(
                new TripointDefinition(
                    tripoint.coordinate(),
                    tripoint.countries().stream().map(Country::name).toList())));
  }

  @Test
  void sameCountriesAtDifferentCoordinatesGetDifferentKeys() {
    List<String> names = List.of("A", "B", "C");
    TripointDefinition first = new TripointDefinition(new Coordinate(1, 2), names);
    TripointDefinition second = new TripointDefinition(new Coordinate(1, 2.5), names);

    assertThat(Tripoint.roundKey(first)).isNotEqualTo(Tripoint.roundKey(second));
    assertThat(Tripoint.subjectId(first.countryNames()))
        .isEqualTo(Tripoint.subjectId(second.countryNames()));
  }

  @Test
  void skipsExcludedSubjectsWithoutLoadingTheirCountries() {
    Set<String> excluded = Set.of("tripoint:A0|B0|C0", "tripoint:A1|B1|C1");

    List<GameData> result = service.generate(3, excluded);

    assertThat(result).extracting(GameData::subjectId).doesNotContainAnyElementsOf(excluded);
    verify(countryLoader, never()).loadDisplayCountry("A0");
    verify(countryLoader, never()).loadDisplayCountry("A1");
    assertThatThrownBy(() -> service.generate(4, excluded))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
