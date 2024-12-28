package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.parkinson.benjamin.geogames.helper.TripointHelper.getCoordinates;

public class TripointComputer {

  public List<Tripoint> computeTripoints(List<Country> countries) {
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

    return
        countriesByCoordinate.entrySet().stream()
            .filter(entry -> entry.getValue().size() == 3)
            .map(entry -> new Tripoint(entry.getKey(), entry.getValue()))
            .toList();
  }

}
