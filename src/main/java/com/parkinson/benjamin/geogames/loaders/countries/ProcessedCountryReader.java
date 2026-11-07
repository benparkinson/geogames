package com.parkinson.benjamin.geogames.loaders.countries;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import tools.jackson.databind.ObjectMapper;

/** Reads every processed per-country json file in a directory. */
public class ProcessedCountryReader {

  private final ObjectMapper objectMapper = GeoJson.lenientReader();

  public List<Country> readCountries(Path directory) throws IOException {
    List<Path> files;
    try (Stream<Path> stream = Files.list(directory)) {
      files = stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
    }
    List<Country> countries = new ArrayList<>(files.size());
    for (Path file : files) {
      countries.add(objectMapper.readValue(file.toFile(), Country.class));
    }
    return countries;
  }
}
