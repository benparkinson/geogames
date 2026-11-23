package com.parkinson.benjamin.geogames.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolygonCentroidTest {

  private static List<List<Number>> square(double west, double south, double size) {
    return List.of(
        List.of(west, south),
        List.of(west + size, south),
        List.of(west + size, south + size),
        List.of(west, south + size),
        List.of(west, south));
  }

  private static GeoData polygon(List<List<Number>> ring) {
    return new GeoData("Feature", null, new GeoGeometry("Polygon", List.of(ring)));
  }

  @Test
  void aUnitSquareGivesItsCentre() {
    double[] position = PolygonCentroid.of(polygon(square(10, 20, 2)));

    assertEquals(21.0, position[0], 0.001);
    assertEquals(11.0, position[1], 0.001);
  }

  @Test
  void theLargestPartOfAMultiPolygonWins() {
    GeoData multi =
        new GeoData(
            "Feature",
            null,
            new GeoGeometry(
                "MultiPolygon", List.of(List.of(square(-55, 4, 1)), List.of(square(0, 40, 8)))));

    double[] position = PolygonCentroid.of(multi);

    assertEquals(44.0, position[0], 0.001);
    assertEquals(4.0, position[1], 0.001);
  }

  @Test
  void aRingWithNoAreaGivesTheAverageOfItsPoints() {
    double[] position =
        PolygonCentroid.of(polygon(List.of(List.of(0, 0), List.of(2, 2), List.of(4, 4))));

    assertEquals(2.0, position[0], 0.001);
    assertEquals(2.0, position[1], 0.001);
  }

  @Test
  void emptyPolygonsAreSkippedAndAnOutlineWithNoneIsRejected() {
    GeoData withEmpty =
        new GeoData(
            "Feature",
            null,
            new GeoGeometry("MultiPolygon", List.of(List.of(), List.of(square(0, 0, 2)))));
    GeoData onlyEmpty =
        new GeoData("Feature", null, new GeoGeometry("MultiPolygon", List.of(List.of())));

    assertEquals(1.0, PolygonCentroid.of(withEmpty)[0], 0.001);
    assertThrows(IllegalArgumentException.class, () -> PolygonCentroid.of(onlyEmpty));
  }

  @Test
  void ringDirectionDoesNotMatter() {
    List<List<Number>> anticlockwise = square(0, 0, 4);
    List<List<Number>> clockwise = anticlockwise.reversed();

    assertEquals(
        PolygonCentroid.of(polygon(anticlockwise))[0], PolygonCentroid.of(polygon(clockwise))[0]);
    assertEquals(
        PolygonCentroid.of(polygon(anticlockwise))[1], PolygonCentroid.of(polygon(clockwise))[1]);
    assertEquals(2.0, PolygonCentroid.of(polygon(clockwise))[1], 0.001);
  }
}
