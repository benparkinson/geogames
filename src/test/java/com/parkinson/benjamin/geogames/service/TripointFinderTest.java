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
import com.parkinson.benjamin.geogames.model.CountryShape;
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
    when(countryLoader.loadShape(anyString()))
        .thenAnswer(
            invocation -> {
              String name = invocation.getArgument(0);
              return new CountryShape(name, List.of(), 0, 0, List.of(), null, null, null, null);
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
        verify(countryLoader).loadShape(name);
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
    when(countryLoader.loadShape(anyString()))
        .thenThrow(new UncheckedIOException(new IOException("boom")));

    assertThatThrownBy(() -> service.generate(1, Set.of()))
        .isInstanceOf(UncheckedIOException.class)
        .hasRootCauseMessage("boom");
  }

  @Test
  void skipsExcludedSubjectsWithoutLoadingTheirCountries() {
    Set<String> excluded = Set.of("tripoint:A0|B0|C0", "tripoint:A1|B1|C1");

    List<GameData> result = service.generate(3, excluded);

    assertThat(result).extracting(GameData::subjectId).doesNotContainAnyElementsOf(excluded);
    verify(countryLoader, never()).loadShape("A0");
    verify(countryLoader, never()).loadShape("A1");
    assertThatThrownBy(() -> service.generate(4, excluded))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
