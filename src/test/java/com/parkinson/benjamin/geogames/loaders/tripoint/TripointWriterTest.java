package com.parkinson.benjamin.geogames.loaders.tripoint;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class TripointWriterTest {

  @TempDir Path directory;

  @Test
  void outputParsesBackIntoTheSameDefinitions() throws IOException {
    List<TripointDefinition> definitions =
        List.of(
            new TripointDefinition(new Coordinate(47.5, 9.5), List.of("A", "B", "C")),
            new TripointDefinition(new Coordinate(-10.25, -20.75), List.of("D", "E", "F")));
    Path file = directory.resolve("tripoints.json");

    new TripointWriter().writeTripoints(definitions, file);

    TripointDefinition[] read =
        JsonMapper.builder().build().readValue(file.toFile(), TripointDefinition[].class);
    assertThat(read).containsExactlyElementsOf(definitions);
  }
}
