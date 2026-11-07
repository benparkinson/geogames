package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TripointFinderTest {

  private TripointLoaderService loader;
  private CountryLoaderService countryLoader;
  private TripointFinderService service;
  private List<TripointDefinition> definitions;

  @BeforeEach
  void setUp() throws IOException {
    loader = mock(TripointLoaderService.class);
    countryLoader = mock(CountryLoaderService.class);
    service = new TripointFinderService(loader, countryLoader);

    definitions =
        IntStream.range(0, 5)
            .mapToObj(
                i ->
                    new TripointDefinition(
                        new Coordinate(i, i * 2), List.of("A" + i, "B" + i, "C" + i)))
            .toList();
    when(loader.loadTripointDefinitions()).thenReturn(definitions);
    when(countryLoader.loadCountryByName(anyString()))
        .thenAnswer(
            invocation -> {
              String name = invocation.getArgument(0);
              return new Country(name, List.of(), null);
            });
  }

  @Test
  public void rejectsRequestForTooManyTripoints() throws IOException {
    TripointLoaderService emptyLoader = mock(TripointLoaderService.class);
    when(emptyLoader.loadTripointDefinitions()).thenReturn(List.of());
    TripointFinderService emptyService =
        new TripointFinderService(emptyLoader, mock(CountryLoaderService.class));

    assertThrows(IllegalArgumentException.class, () -> emptyService.findRandomTripoints(1));
  }

  @Test
  void returnsDistinctTripointsBuiltFromDefinitions() throws IOException {
    List<GameData> result = service.findRandomTripoints(3);

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
        verify(countryLoader).loadCountryByName(name);
      }
    }
  }

  @Test
  void canReturnEveryDefinition() throws IOException {
    List<GameData> result = service.findRandomTripoints(definitions.size());

    assertThat(result.stream().map(t -> ((Tripoint) t).coordinate()).toList())
        .containsExactlyInAnyOrderElementsOf(
            definitions.stream().map(TripointDefinition::coordinate).toList());
  }

  @Test
  void returnsNothingWithoutLoadingCountriesWhenAskedForZero() throws IOException {
    assertThat(service.findRandomTripoints(0)).isEmpty();
    verifyNoInteractions(countryLoader);
  }

  @Test
  void propagatesCountryLoadingFailures() throws IOException {
    when(countryLoader.loadCountryByName(anyString())).thenThrow(new IOException("boom"));

    assertThatThrownBy(() -> service.findRandomTripoints(1))
        .isInstanceOf(IOException.class)
        .hasMessage("boom");
  }
}
