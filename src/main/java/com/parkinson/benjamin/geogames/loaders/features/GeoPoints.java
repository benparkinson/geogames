package com.parkinson.benjamin.geogames.loaders.features;

import java.util.ArrayList;
import java.util.List;

/** Rounding and converting the {lon, lat} points the loaders work with. */
public final class GeoPoints {

  private GeoPoints() {}

  /** Rounds to 4 decimal places (about 11 m), the precision of every generated file. */
  public static double round4(double value) {
    return Math.round(value * 10_000d) / 10_000d;
  }

  /** Rounds to 4 decimals and drops a point equal to the one before it. */
  public static List<double[]> roundLine(List<double[]> points) {
    List<double[]> rounded = new ArrayList<>();
    for (double[] p : points) {
      double[] r = {round4(p[0]), round4(p[1])};
      double[] last = rounded.isEmpty() ? null : rounded.get(rounded.size() - 1);
      if (last == null || last[0] != r[0] || last[1] != r[1]) {
        rounded.add(r);
      }
    }
    return rounded;
  }

  /** The points as GeoJSON positions. */
  public static List<List<Double>> toPositions(List<double[]> points) {
    List<List<Double>> positions = new ArrayList<>(points.size());
    for (double[] p : points) {
      positions.add(List.of(p[0], p[1]));
    }
    return positions;
  }
}
