package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TripointWriter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public void writeTripoints(List<TripointDefinition> tripoints, Path destinationFile)
      throws IOException {
    String value = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tripoints);
    Files.write(destinationFile, List.of(value));
  }
}
