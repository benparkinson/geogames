package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.ArrayList;
import java.util.List;

/**
 * The position of a river for distance clues: its vertex nearest to its length-weighted centroid.
 * The pieces of a river are not in order, so "halfway along" is unreliable, and the plain centroid
 * of a curved river falls off it. Snapping to a vertex keeps the point on the river and near its
 * middle. Lengths and distances scale longitude by the cosine of the mean latitude.
 */
public final class RiverPosition {

  private RiverPosition() {}

  /**
   * @param line a MultiLineString
   * @return {@code {lat, lon}}, rounded to 2 decimal places
   * @throws IllegalArgumentException if the geometry has no vertices
   */
  public static double[] of(GeoData line) {
    List<List<double[]>> pieces = pieces(line);
    List<double[]> vertices = pieces.stream().flatMap(List::stream).toList();
    if (vertices.isEmpty()) {
      throw new IllegalArgumentException("Line has no vertices");
    }
    double scale =
        Math.cos(Math.toRadians(vertices.stream().mapToDouble(p -> p[1]).average().orElse(0)));
    double total = 0;
    double cx = 0;
    double cy = 0;
    for (List<double[]> piece : pieces) {
      for (int i = 1; i < piece.size(); i++) {
        double[] a = piece.get(i - 1);
        double[] b = piece.get(i);
        double length = Math.hypot((b[0] - a[0]) * scale, b[1] - a[1]);
        total += length;
        cx += (a[0] + b[0]) / 2 * length;
        cy += (a[1] + b[1]) / 2 * length;
      }
    }
    if (total == 0) {
      cx = vertices.get(0)[0];
      cy = vertices.get(0)[1];
    } else {
      cx /= total;
      cy /= total;
    }
    double[] nearest = vertices.get(0);
    double best = Double.MAX_VALUE;
    for (double[] vertex : vertices) {
      double distance = Math.hypot((vertex[0] - cx) * scale, vertex[1] - cy);
      if (distance < best) {
        best = distance;
        nearest = vertex;
      }
    }
    return new double[] {round2(nearest[1]), round2(nearest[0])};
  }

  private static List<List<double[]>> pieces(GeoData line) {
    List<?> coordinates = line.geometry().coordinates();
    List<List<double[]>> pieces = new ArrayList<>();
    for (Object piece : coordinates) {
      List<double[]> points = new ArrayList<>();
      for (Object point : (List<?>) piece) {
        List<?> position = (List<?>) point;
        points.add(
            new double[] {
              ((Number) position.get(0)).doubleValue(), ((Number) position.get(1)).doubleValue()
            });
      }
      pieces.add(points);
    }
    return pieces;
  }

  private static double round2(double v) {
    return Math.round(v * 100) / 100.0;
  }
}
