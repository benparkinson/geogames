package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TripointLoaderService extends FileLoaderService {

  @Cacheable("tripoints")
  public List<Tripoint> loadTripoints() throws IOException {
    Tripoint[] tripoints = loadFile("processed/tripoints.json", Tripoint[].class);

    return Arrays.asList(tripoints);
  }
}
