package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class TripointWriter {

  private final ObjectMapper objectMapper =
      JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

  public void writeTripoints(List<TripointDefinition> tripoints, Path destinationFile)
      throws IOException {
    String value = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tripoints);
    Files.write(destinationFile, List.of(value));
  }
}
