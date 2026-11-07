package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TripointHelperTest {

  @Test
  public void findsPolygonCoordinates() {
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(12.34, 56.78)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    List<Coordinate> output = TripointHelper.getCoordinates(geometry);

    assertThat(output).hasSize(1);
    assertThat(output).containsOnly(new Coordinate(56.78, 12.34));
  }

  @Test
  public void findsMultiPolygonCoordinates() {
    List<List<List<List<Double>>>> coordinates = List.of(List.of(List.of(List.of(12.34, 56.78))));
    GeoGeometry geometry = new GeoGeometry("MultiPolygon", coordinates);

    List<Coordinate> output = TripointHelper.getCoordinates(geometry);

    assertThat(output).hasSize(1);
    assertThat(output).containsOnly(new Coordinate(56.78, 12.34));
  }

  @Test
  public void flagsIllegalCoordinates() {
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(12.34, 56.78, 43.21)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    assertThrows(IllegalArgumentException.class, () -> TripointHelper.getCoordinates(geometry));
  }

  @Test
  public void findsMultiLineStringCoordinates() {
    List<List<List<Double>>> coordinates = List.of(List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)));
    GeoGeometry geometry = new GeoGeometry("MultiLineString", coordinates);

    assertThat(TripointHelper.getCoordinates(geometry))
        .containsExactly(new Coordinate(2.0, 1.0), new Coordinate(4.0, 3.0));
  }

  @Test
  public void flattensMultipleRingsAndPolygonsInOrder() {
    List<List<List<List<Double>>>> coordinates =
        List.of(
            List.of(List.of(List.of(1.0, 2.0)), List.of(List.of(3.0, 4.0))),
            List.of(List.of(List.of(5.0, 6.0))));
    GeoGeometry geometry = new GeoGeometry("MultiPolygon", coordinates);

    assertThat(TripointHelper.getCoordinates(geometry))
        .containsExactly(
            new Coordinate(2.0, 1.0), new Coordinate(4.0, 3.0), new Coordinate(6.0, 5.0));
  }

  @Test
  public void convertsIntegerCoordinatesToDoubles() {
    List<List<List<Integer>>> coordinates = List.of(List.of(List.of(10, 20)));
    GeoGeometry geometry = new GeoGeometry("Polygon", coordinates);

    assertThat(TripointHelper.getCoordinates(geometry)).containsExactly(new Coordinate(20.0, 10.0));
  }

  @Test
  public void emptyGeometryGivesNoCoordinates() {
    assertThat(TripointHelper.getCoordinates(new GeoGeometry("Polygon", List.of()))).isEmpty();
  }
}
