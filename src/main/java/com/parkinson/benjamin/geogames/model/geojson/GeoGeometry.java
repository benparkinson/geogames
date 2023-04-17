package com.parkinson.benjamin.geogames.model.geojson;

import java.util.List;

public record GeoGeometry(String type, List coordinates) {

  private static final String POLYGON = "Polygon";
  private static final String MULTI_POLYGON = "MultiPolygon";
  private static final String MULTI_LINE_STRING = "MultiLineString";

  public GeoGeometry {
    if (!type.equals(POLYGON) && !type.equals(MULTI_POLYGON)
        && !type.equals(MULTI_LINE_STRING)) {
      throw new IllegalArgumentException();
    }
  }

  public List<List<Number>> flattenedCoordinates() {
    if (this.type.equals(POLYGON) || this.type.equals(MULTI_LINE_STRING)) {
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
