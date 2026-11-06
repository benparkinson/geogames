package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TripointLoaderService extends FileLoaderService {

  @Cacheable("tripointDefinitions")
  public List<TripointDefinition> loadTripointDefinitions() throws IOException {
    return Arrays.asList(loadFile("processed/tripoints.json", TripointDefinition[].class));
  }
}
