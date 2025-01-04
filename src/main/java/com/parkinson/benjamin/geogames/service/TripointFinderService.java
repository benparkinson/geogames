package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TripointFinderService {

  private final Random random = new Random(System.currentTimeMillis());

  private final TripointLoaderService tripointLoaderService;

  @Autowired
  public TripointFinderService(TripointLoaderService tripointLoaderService) {
    this.tripointLoaderService = tripointLoaderService;
  }

  public List<GameData> findRandomTripoints(int totalTripoints, int howMany) throws IOException {
    if (totalTripoints < howMany) {
      throw new IllegalArgumentException(
          "Can't give you %d tripoints, I only know about %d!"
              .formatted(howMany, totalTripoints));
    }

    Set<GameData> tripoints = new HashSet<>();
    while (tripoints.size() < howMany) {
      Tripoint tripoint = tripointLoaderService.loadTripointByIndex(random.nextInt(totalTripoints));
      tripoints.add(tripoint);
    }

    return tripoints.stream().toList();
  }
}
