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

  public Tripoint findTripoint(List<Country> countries) {
    List<Tripoint> allTripoints = findAllTripoints(countries);

    return allTripoints.get(random.nextInt(allTripoints.size()));
  }

  public List<GameData> findRandomTripoints(List<Country> countries, int howMany) {
    List<Tripoint> allTripoints = findAllTripoints(countries);

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

  @NotNull
  private static List<Tripoint> findAllTripoints(List<Country> countries) {
    Map<Coordinate, Set<Country>> countriesByCoordinate = new HashMap<>();

    countries.forEach(
        country ->
            getCoordinates(country.geoData().geometry())
                .forEach(
                    coordinate -> {
                      Set<Country> countriesWithCoordinate =
                          countriesByCoordinate.computeIfAbsent(coordinate, k -> new HashSet<>());
                      countriesWithCoordinate.add(country);
                    }));

    List<Tripoint> allTripoints =
        countriesByCoordinate.entrySet().stream()
            .filter(entry -> entry.getValue().size() == 3)
            .map(entry -> new Tripoint(entry.getKey(), entry.getValue()))
            .toList();
    return allTripoints;
  }
}
