package com.parkinson.benjamin.geogames.model.geojson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

public class GeoGeometryTest {

  @Test
  public void polygonCoordinates() {
    List<List<List<Double>>> coordinates =
        List.of(List.of(List.of(12.34, 56.78), List.of(78.9, 12.34)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    List<List<Number>> flattenedCoordinates = geometry.flattenedCoordinates();

    assertThat(flattenedCoordinates).hasSize(2);
    assertThat(flattenedCoordinates).containsOnly(List.of(12.34, 56.78), List.of(78.9, 12.34));
  }

  @Test
  public void multiLineStringCoordinates() {
    List<List<List<Double>>> coordinates =
        List.of(List.of(List.of(12.34, 56.78), List.of(78.9, 12.34)));
    GeoGeometry geometry = new GeoGeometry("MultiLineString", coordinates);

    List<List<Number>> flattenedCoordinates = geometry.flattenedCoordinates();

    assertThat(flattenedCoordinates).hasSize(2);
    assertThat(flattenedCoordinates).containsOnly(List.of(12.34, 56.78), List.of(78.9, 12.34));
  }

  @Test
  public void multiPolygonCoordinates() {
    List<List<List<List<Double>>>> coordinates =
        List.of(List.of(List.of(List.of(12.34, 56.78), List.of(78.9, 12.34))));
    GeoGeometry geometry = new GeoGeometry("MultiPolygon", coordinates);

    List<List<Number>> flattenedCoordinates = geometry.flattenedCoordinates();

    assertThat(flattenedCoordinates).hasSize(2);
    assertThat(flattenedCoordinates).containsOnly(List.of(12.34, 56.78), List.of(78.9, 12.34));
  }
}
