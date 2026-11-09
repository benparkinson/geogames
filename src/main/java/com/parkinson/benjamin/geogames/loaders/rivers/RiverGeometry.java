package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.loaders.features.DouglasPeucker;
import com.parkinson.benjamin.geogames.loaders.features.Geo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Simplifies and measures a river's lines. A line is a list of {lon, lat} points. */
public final class RiverGeometry {

  static final double START_TOLERANCE = 0.005;
  static final double MAX_TOLERANCE = 0.16;
  static final int MAX_POINTS = 600;
  static final double MIN_LINE_KM = 5;

  private RiverGeometry() {}

  public static List<List<double[]>> simplify(List<List<double[]>> lines) {
    double tolerance = START_TOLERANCE;
    List<List<double[]>> result = simplifyAll(lines, tolerance);
    while (pointCount(result) > MAX_POINTS && tolerance < MAX_TOLERANCE - 1e-9) {
      tolerance *= 2;
      result = simplifyAll(lines, tolerance);
    }

    result = dropShortLines(result);

    // Safety net so the point cap always holds: drop the shortest lines until it does.
    while (pointCount(result) > MAX_POINTS && result.size() > 1) {
      List<double[]> shortest = result.stream().min(Comparator.comparingDouble(Geo::lineKm)).get();
      result = new ArrayList<>(result);
      result.remove(shortest);
    }
    return round(result);
  }

  /**
   * The lines of the river's main piece: lines are joined into pieces when an end of one is within
   * {@link RiverEnds#GAP_KM} of an end of another, and the piece with the most km is kept. The kept
   * lines stay in their original order.
   */
  public static List<List<double[]>> mainPiece(List<List<double[]>> lines) {
    List<List<double[]>> usable = lines.stream().filter(line -> line.size() >= 2).toList();
    int n = usable.size();
    if (n < 2) {
      return usable;
    }
    int[] piece = new int[n];
    for (int i = 0; i < n; i++) {
      piece[i] = i;
    }
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        if (endsMeet(usable.get(i), usable.get(j))) {
          int from = piece[j];
          int to = piece[i];
          for (int k = 0; k < n; k++) {
            if (piece[k] == from) {
              piece[k] = to;
            }
          }
        }
      }
    }
    double[] km = new double[n];
    for (int i = 0; i < n; i++) {
      km[piece[i]] += Geo.lineKm(usable.get(i));
    }
    int main = 0;
    for (int i = 1; i < n; i++) {
      if (km[i] > km[main]) {
        main = i;
      }
    }
    List<List<double[]>> result = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      if (piece[i] == main) {
        result.add(usable.get(i));
      }
    }
    return result;
  }

  private static boolean endsMeet(List<double[]> a, List<double[]> b) {
    for (double[] p : List.of(a.get(0), a.get(a.size() - 1))) {
      for (double[] q : List.of(b.get(0), b.get(b.size() - 1))) {
        if (Geo.haversineKm(p, q) <= RiverEnds.GAP_KM) {
          return true;
        }
      }
    }
    return false;
  }

  public static double lengthKm(List<List<double[]>> lines) {
    return lines.stream().mapToDouble(Geo::lineKm).sum();
  }

  public static int pointCount(List<List<double[]>> lines) {
    return lines.stream().mapToInt(List::size).sum();
  }

  private static List<List<double[]>> simplifyAll(List<List<double[]>> lines, double tolerance) {
    List<List<double[]>> result = new ArrayList<>();
    for (List<double[]> line : lines) {
      if (line.size() >= 2) {
        result.add(DouglasPeucker.simplify(line, tolerance));
      }
    }
    return result;
  }

  private static List<List<double[]>> dropShortLines(List<List<double[]>> lines) {
    if (lines.isEmpty()) {
      return lines;
    }
    List<double[]> longest = lines.stream().max(Comparator.comparingDouble(Geo::lineKm)).get();
    List<List<double[]>> result = new ArrayList<>();
    for (List<double[]> line : lines) {
      if (line == longest || Geo.lineKm(line) >= MIN_LINE_KM) {
        result.add(line);
      }
    }
    return result;
  }

  private static List<List<double[]>> round(List<List<double[]>> lines) {
    List<List<double[]>> result = new ArrayList<>();
    for (List<double[]> line : lines) {
      List<double[]> rounded = new ArrayList<>();
      for (double[] p : line) {
        double[] r = {round4(p[0]), round4(p[1])};
        if (rounded.isEmpty()
            || rounded.get(rounded.size() - 1)[0] != r[0]
            || rounded.get(rounded.size() - 1)[1] != r[1]) {
          rounded.add(r);
        }
      }
      if (rounded.size() >= 2) {
        result.add(rounded);
      }
    }
    return result;
  }

  private static double round4(double v) {
    return Math.round(v * 10_000d) / 10_000d;
  }
}
