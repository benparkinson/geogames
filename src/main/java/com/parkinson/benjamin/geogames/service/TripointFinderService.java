package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TripointFinderService {

  public Tripoint findTripoint(List<Country> countries) {
    Random random = new Random(System.currentTimeMillis());

    Map<Coordinate, Set<Country>> countriesByCoordinate = new HashMap<>();

    countries.forEach(country -> Arrays.stream(country.getCoordinates()).forEach(coordinate -> {
      Set<Country> countriesWithCoordinate = countriesByCoordinate
          .computeIfAbsent(coordinate, k -> new HashSet<>());
      countriesWithCoordinate.add(country);
    }));

    List<Tripoint> allTripoints = countriesByCoordinate.entrySet().stream()
        .filter(entry -> entry.getValue().size() == 3)
        .map(entry -> new Tripoint(entry.getKey(),
            entry.getValue()))
        .toList();

    return allTripoints.get(random.nextInt(allTripoints.size()));
  }

}
