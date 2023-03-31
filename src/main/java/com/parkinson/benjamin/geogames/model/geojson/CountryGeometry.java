package com.parkinson.benjamin.geogames.model.geojson;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CountryGeometry {

  private static final String POLYGON = "Polygon";
  private static final String MULTI_POLYGON = "MultiPolygon";

  private String type;

  private List coordinates;

  public CountryGeometry(String type, List coordinates) {
    if (!type.equals(POLYGON) && !type.equals(MULTI_POLYGON)) {
      throw new IllegalArgumentException();
    }
    this.type = type;
    this.coordinates = coordinates;
  }

  public List<List<Number>> getAllCoordinates() {
    if (this.type.equals(POLYGON)) {
      return getPolygonCoordinates();
    } else if (this.type.equals(MULTI_POLYGON)) {
      return getMultiPolygonCoordinates();
    } else {
      return List.of();
    }
  }

  private List<List<Number>> getMultiPolygonCoordinates() {
    List<List<List<List<Number>>>> realCoords = coordinates;
    return realCoords.stream()
        .flatMap(List::stream)
        .flatMap(List::stream)
        .toList();
  }

  private List<List<Number>> getPolygonCoordinates() {
    List<List<List<Number>>> realCoords = coordinates;
    return realCoords.stream()
        .flatMap(List::stream)
        .toList();
  }

}
