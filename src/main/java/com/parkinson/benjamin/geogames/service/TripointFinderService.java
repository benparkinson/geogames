package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.helper.TripointHelper.getCoordinates;

import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class TripointFinderService {

  private final Random random = new Random(System.currentTimeMillis());

  public List<GameData> findRandomTripoints(List<Tripoint> allTripoints, int howMany) {
    if (allTripoints.size() < howMany) {
      throw new IllegalArgumentException(
          "Can't give you %d tripoints, I only know about %d!"
              .formatted(howMany, allTripoints.size()));
    }

    Set<GameData> tripoints = new HashSet<>();
    while (tripoints.size() < howMany) {
      Tripoint tripoint = allTripoints.get(random.nextInt(allTripoints.size()));
      tripoints.add(tripoint);
    }

    return tripoints.stream().toList();
  }
}
