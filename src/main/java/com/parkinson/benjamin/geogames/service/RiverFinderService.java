package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.River;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RiverFinderService {

  private final Random random = new Random(System.currentTimeMillis());

  public River findRiver(List<River> rivers) {
    return rivers.get(random.nextInt(rivers.size()));
  }

  public List<River> findRandomRivers(List<River> rivers, int howMany) {
    if (rivers.size() < howMany) {
      throw new IllegalArgumentException(
          "Can't give you %d rivers, I only know about %d!".formatted(howMany,
              rivers.size()));
    }

    Set<River> randomRivers = new HashSet<>();
    while (randomRivers.size() < howMany) {
      River river = rivers.get(random.nextInt(rivers.size()));
      randomRivers.add(river);
    }

    return randomRivers.stream().toList();
  }

}
