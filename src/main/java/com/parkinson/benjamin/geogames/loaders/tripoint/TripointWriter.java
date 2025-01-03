package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TripointWriter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public void writeTripoints(List<Tripoint> tripoints, Path destination) throws IOException {
    String value = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tripoints);

    Files.write(destination, List.of(value));
  }

}
