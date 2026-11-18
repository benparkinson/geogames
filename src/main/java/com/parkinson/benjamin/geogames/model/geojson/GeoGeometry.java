package com.parkinson.benjamin.geogames.model.geojson;

import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;

public record GeoGeometry(
    String type, @JsonDeserialize(using = CoordinatesDeserializer.class) List coordinates) {

  private static final String POLYGON = "Polygon";
  private static final String MULTI_POLYGON = "MultiPolygon";
  private static final String MULTI_LINE_STRING = "MultiLineString";

  public GeoGeometry {
    if (!type.equals(POLYGON) && !type.equals(MULTI_POLYGON) && !type.equals(MULTI_LINE_STRING)) {
      throw new IllegalArgumentException();
    }
  }

  /** Receives each position of a geometry. */
  @FunctionalInterface
  public interface PositionVisitor {
    void visit(double lon, double lat);
  }

  /**
   * Visits every position in a Polygon, MultiPolygon or MultiLineString, in order. A list whose
   * first element is a number is a position; an empty list has none. (Not named {@code getX}, so
   * the serialised JSON gains no field.)
   */
  public void forEachPosition(PositionVisitor visitor) {
    walk(coordinates, visitor);
  }

  /** How many positions {@link #forEachPosition} visits. */
  public int positionCount() {
    int[] count = {0};
    forEachPosition((lon, lat) -> count[0]++);
    return count[0];
  }

  private static void walk(List<?> list, PositionVisitor visitor) {
    if (list.isEmpty()) {
      return;
    }
    if (list.get(0) instanceof Number) {
      visitor.visit(((Number) list.get(0)).doubleValue(), ((Number) list.get(1)).doubleValue());
      return;
    }
    for (Object element : list) {
      walk((List<?>) element, visitor);
    }
  }

  public List<List<Number>> flattenedCoordinates() {
    if (this.type.equals(POLYGON) || this.type.equals(MULTI_LINE_STRING)) {
      return getPolygonCoordinates();
    }
    return getMultiPolygonCoordinates();
  }

  private List<List<Number>> getMultiPolygonCoordinates() {
    List<List<List<List<Number>>>> realCoords = coordinates;
    return realCoords.stream().flatMap(List::stream).flatMap(List::stream).toList();
  }

  private List<List<Number>> getPolygonCoordinates() {
    List<List<List<Number>>> realCoords = coordinates;
    return realCoords.stream().flatMap(List::stream).toList();
  }
}
