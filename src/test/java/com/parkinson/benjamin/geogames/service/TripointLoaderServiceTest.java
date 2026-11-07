package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class TripointLoaderServiceTest {

  private final TripointLoaderService service = new TripointLoaderService();

  @Test
  void loadsAllDefinitionsWithValidCoordinates() throws IOException {
    List<TripointDefinition> definitions = service.loadTripointDefinitions();

    assertThat(definitions).hasSize(164);
    for (TripointDefinition definition : definitions) {
      assertThat(definition.coordinate().latitude()).isBetween(-90.0, 90.0);
      assertThat(definition.coordinate().longitude()).isBetween(-180.0, 180.0);
    }
  }
}
