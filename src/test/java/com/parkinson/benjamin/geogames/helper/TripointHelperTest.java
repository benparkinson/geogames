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
}
