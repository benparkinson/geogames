package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class TripointLoaderService extends FileLoaderService {

  public int countTripoints() {
    // can't count files when running as a .jar
    // todo write a metadata file of tripoints and save the number there?
    return 170;
  }

  public Tripoint loadTripointByIndex(int index) throws IOException {
    return loadFile("processed/tripoints/" + (index + 1) + ".json", Tripoint.class);
  }
}
