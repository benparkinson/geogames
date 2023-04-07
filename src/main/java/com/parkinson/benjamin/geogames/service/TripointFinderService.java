package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TripointFinderService {

  private final Random random = new Random(System.currentTimeMillis());

  public Tripoint findTripoint(List<GeoData> countries) {
    Map<Coordinate, Set<GeoData>> countriesByCoordinate = new HashMap<>();

    countries.forEach(country -> getCoordinates(country.getGeometry())
        .forEach(coordinate -> {
          Set<GeoData> countriesWithCoordinate = countriesByCoordinate
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

  public List<Coordinate> getCoordinates(GeoGeometry geoGeometry) {

    List<List<Number>> allCoordinates = geoGeometry.flattenedCoordinates();

    return allCoordinates.stream().map(coordList -> {
      if (coordList.size() != 2) {
        throw new IllegalArgumentException("This is not a coordinate");
      }

      return new Coordinate(coordList.get(1).doubleValue(), coordList.get(0).doubleValue());
    }).toList();
  }

}
