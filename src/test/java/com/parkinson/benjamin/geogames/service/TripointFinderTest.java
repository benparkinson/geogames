package com.parkinson.benjamin.geogames.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TripointFinderTest {

  @Test
  public void rejectsRequestForTooManyTripoints() throws IOException {
    TripointLoaderService loader = mock(TripointLoaderService.class);
    when(loader.loadTripointDefinitions()).thenReturn(List.of());
    TripointFinderService service =
        new TripointFinderService(loader, mock(CountryLoaderService.class));

    assertThrows(IllegalArgumentException.class, () -> service.findRandomTripoints(1));
  }
}
