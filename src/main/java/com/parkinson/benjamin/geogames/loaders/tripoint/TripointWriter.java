package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TripointWriter {

  public void writeTripoints(List<TripointDefinition> tripoints, Path destinationFile)
      throws IOException {
    String value = GeoJson.prettyWriter().writeValueAsString(tripoints);
    Files.write(destinationFile, List.of(value));
  }
}
