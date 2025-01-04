package com.parkinson.benjamin.geogames.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TripointFinderTest {

  private final TripointFinderService service = new TripointFinderService(null);

  @Test
  public void rejectsRequestForTooManyTripoints() {
    assertThrows(IllegalArgumentException.class, () -> service.findRandomTripoints(0, 1));
  }
}
