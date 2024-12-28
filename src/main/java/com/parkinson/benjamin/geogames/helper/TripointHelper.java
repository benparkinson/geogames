package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public final class TripointHelper {

  private TripointHelper() {}

  public static List<Coordinate> getCoordinates(GeoGeometry geoGeometry) {

    List<List<Number>> allCoordinates = geoGeometry.flattenedCoordinates();

    return allCoordinates.stream()
        .map(
            coordList -> {
              if (coordList.size() != 2) {
                throw new IllegalArgumentException("This is not a coordinate");
              }

              return new Coordinate(coordList.get(1).doubleValue(), coordList.get(0).doubleValue());
            })
        .collect(Collectors.toCollection(LinkedList::new));
  }
}
