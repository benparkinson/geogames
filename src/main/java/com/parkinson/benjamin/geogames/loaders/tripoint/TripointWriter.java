package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TripointWriter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public void writeTripoints(List<Tripoint> tripoints, Path destinationFolder) throws IOException {

    for (int i = 0; i < tripoints.size(); i++) {
      String value =
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tripoints.get(i));

      Files.write(Path.of(destinationFolder.toString(), (i + 1) + ".json"), List.of(value));
    }
  }
}
