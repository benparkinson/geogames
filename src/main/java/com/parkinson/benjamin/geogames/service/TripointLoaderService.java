package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TripointLoaderService {

  private final JsonResourceReader reader;

  public TripointLoaderService(JsonResourceReader reader) {
    this.reader = reader;
  }

  @Cacheable("tripointDefinitions")
  public List<TripointDefinition> loadTripointDefinitions() {
    return List.of(reader.read("processed/tripoints.json", TripointDefinition[].class));
  }
}
