package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class TripointLoaderService extends FileLoaderService {
  
  public int countTripoints() throws IOException {
    File folder = new ClassPathResource("processed/tripoints/").getFile();
    File[] files = folder.listFiles();
    return files.length;
  }

  public Tripoint loadTripointByIndex(int index) throws IOException {
      return loadFile("processed/tripoints/" + (index + 1) + ".json", Tripoint.class);
  }
}
