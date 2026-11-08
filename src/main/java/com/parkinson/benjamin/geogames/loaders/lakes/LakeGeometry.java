package com.parkinson.benjamin.geogames.loaders.lakes;

import com.parkinson.benjamin.geogames.loaders.features.DouglasPeucker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Simplifies and measures a lake. A polygon is a list of rings (the outer ring, then holes); a ring
 * is a list of {lon, lat} points.
 */
public final class LakeGeometry {

  static final double START_TOLERANCE = 0.002;
  static final double MAX_TOLERANCE = 0.08;
  static final int MAX_POINTS = 800;

  /** A hole (island) smaller than this share of its outer ring is dropped. */
  static final double MIN_HOLE_SHARE = 0.01;

  /** A polygon smaller than this share of the largest one is dropped. */
  static final double MIN_POLYGON_SHARE = 0.02;

  private static final double KM_PER_DEGREE_LON = 111.32;
  private static final double KM_PER_DEGREE_LAT = 110.57;

  private LakeGeometry() {}

  /** Area in km², holes subtracted. */
  public static double areaKm2(List<List<List<double[]>>> polygons) {
    return polygons.stream().mapToDouble(LakeGeometry::polygonKm2).sum();
  }

  public static int pointCount(List<List<List<double[]>>> polygons) {
    return polygons.stream().flatMap(List::stream).mapToInt(List::size).sum();
  }

  public static List<List<List<double[]>>> simplify(List<List<List<double[]>>> polygons) {
    double tolerance = START_TOLERANCE;
    List<List<List<double[]>>> result = clean(polygons, tolerance);
    while (pointCount(result) > MAX_POINTS && tolerance < MAX_TOLERANCE - 1e-9) {
      tolerance = Math.min(tolerance * 2, MAX_TOLERANCE);
      result = clean(polygons, tolerance);
    }
    return round(result);
  }

  private static List<List<List<double[]>>> clean(
      List<List<List<double[]>>> polygons, double tolerance) {
    List<List<List<double[]>>> simplified = new ArrayList<>();
    for (List<List<double[]>> polygon : polygons) {
      List<List<double[]>> rings = new ArrayList<>();
      for (int i = 0; i < polygon.size(); i++) {
        List<double[]> closed = close(DouglasPeucker.simplify(polygon.get(i), tolerance));
        if (closed.size() >= 4) {
          rings.add(closed);
        } else if (i == 0) {
          break; // without its outer ring the whole polygon goes
        }
      }
      if (!rings.isEmpty()) {
        simplified.add(rings);
      }
    }

    // Drop the holes (islands) that are tiny next to their outer ring.
    List<List<List<double[]>>> kept = new ArrayList<>();
    for (List<List<double[]>> polygon : simplified) {
      double outer = ringKm2(polygon.get(0));
      List<List<double[]>> rings = new ArrayList<>();
      rings.add(polygon.get(0));
      for (List<double[]> hole : polygon.subList(1, polygon.size())) {
        if (ringKm2(hole) >= outer * MIN_HOLE_SHARE) {
          rings.add(hole);
        }
      }
      kept.add(rings);
    }

    // Keep the main body of the lake and drop stray fragments.
    double largest = kept.stream().mapToDouble(LakeGeometry::polygonKm2).max().orElse(0);
    List<List<List<double[]>>> result = new ArrayList<>();
    for (List<List<double[]>> polygon : kept) {
      if (polygonKm2(polygon) >= largest * MIN_POLYGON_SHARE) {
        result.add(polygon);
      }
    }
    result.sort(Comparator.comparingDouble(LakeGeometry::polygonKm2).reversed());
    return result;
  }

  private static List<double[]> close(List<double[]> ring) {
    if (ring.isEmpty()) {
      return ring;
    }
    List<double[]> closed = new ArrayList<>(ring);
    double[] first = closed.get(0);
    double[] last = closed.get(closed.size() - 1);
    if (first[0] != last[0] || first[1] != last[1]) {
      closed.add(first);
    }
    return closed;
  }

  private static double polygonKm2(List<List<double[]>> polygon) {
    double area = ringKm2(polygon.get(0));
    for (List<double[]> hole : polygon.subList(1, polygon.size())) {
      area -= ringKm2(hole);
    }
    return area;
  }

  /** Shoelace formula on points projected to km around the ring's mean latitude. */
  static double ringKm2(List<double[]> ring) {
    double meanLat = ring.stream().mapToDouble(p -> p[1]).average().orElse(0);
    double lonScale = KM_PER_DEGREE_LON * Math.cos(Math.toRadians(meanLat));
    double sum = 0;
    for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
      double xi = ring.get(i)[0] * lonScale;
      double yi = ring.get(i)[1] * KM_PER_DEGREE_LAT;
      double xj = ring.get(j)[0] * lonScale;
      double yj = ring.get(j)[1] * KM_PER_DEGREE_LAT;
      sum += xj * yi - xi * yj;
    }
    return Math.abs(sum) / 2;
  }

  private static List<List<List<double[]>>> round(List<List<List<double[]>>> polygons) {
    List<List<List<double[]>>> result = new ArrayList<>();
    for (List<List<double[]>> polygon : polygons) {
      List<List<double[]>> rings = new ArrayList<>();
      for (List<double[]> ring : polygon) {
        List<double[]> rounded = new ArrayList<>();
        for (double[] p : ring) {
          rounded.add(new double[] {round4(p[0]), round4(p[1])});
        }
        rings.add(rounded);
      }
      result.add(rings);
    }
    return result;
  }

  private static double round4(double v) {
    return Math.round(v * 10_000d) / 10_000d;
  }
}
