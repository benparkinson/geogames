package com.parkinson.benjamin.geogames.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiverPositionTest {

  private static List<Number> p(double lon, double lat) {
    return List.of(lon, lat);
  }

  private static GeoData multi(List<?>... pieces) {
    return new GeoData("Feature", null, new GeoGeometry("MultiLineString", List.of(pieces)));
  }

  @Test
  void aStraightLineGivesItsMiddleVertex() {
    GeoData line = multi(List.of(p(0, 0), p(1, 0), p(2, 0)));

    assertArrayEquals(new double[] {0.0, 1.0}, RiverPosition.of(line), 1e-9);
  }

  @Test
  void aUShapedLineGivesAVertexOnTheLineNotTheOffLineCentroid() {
    // The plain centroid is near (lon 1, lat 3), which is off the river's arms and base.
    GeoData u = multi(List.of(p(0, 10), p(0, 0), p(2, 0), p(2, 10)));

    double[] position = RiverPosition.of(u);

    assertArrayEquals(new double[] {0.0, 0.0}, position, 1e-9);
  }

  @Test
  void piecesOutOfOrderGiveTheSamePoint() {
    List<?> a = List.of(p(0, 0), p(1, 1));
    List<?> b = List.of(p(1, 1), p(2, 3));
    List<?> c = List.of(p(2, 3), p(4, 4));

    assertArrayEquals(RiverPosition.of(multi(a, b, c)), RiverPosition.of(multi(c, a, b)), 1e-9);
  }

  @Test
  void anEmptyGeometryThrows() {
    assertThrows(IllegalArgumentException.class, () -> RiverPosition.of(multi()));
  }
}
