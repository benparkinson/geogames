package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.loaders.features.DouglasPeucker;
import com.parkinson.benjamin.geogames.loaders.features.Geo;
import com.parkinson.benjamin.geogames.loaders.features.GeoPoints;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverClipper.Run;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** The size limits on the rivers of one country: no stubs, and at most {@link #MAX_RIVER_POINTS}. */
final class CountryRiverLimits {

  /** A stretch shorter than this (after simplifying) is a stub, not a river on this map. */
  static final double MIN_RUN_KM = 5;

  /** The most river points a country keeps: the least important rivers are dropped to fit. */
  static final int MAX_RIVER_POINTS = 4_000;

  /** A stretch of river kept for a country, with its length. */
  record Kept(Run run, double km) {}

  private CountryRiverLimits() {}

  /** Simplifies every run with the country's tolerance and drops the stubs. */
  static List<Kept> simplified(List<Run> runs, double tolerance) {
    List<Kept> kept = new ArrayList<>();
    for (Run run : runs) {
      List<double[]> points = GeoPoints.roundLine(DouglasPeucker.simplify(run.points(), tolerance));
      if (points.size() < 2) {
        continue;
      }
      double km = Geo.lineKm(points);
      if (km >= MIN_RUN_KM) {
        kept.add(new Kept(new Run(run.scalerank(), run.rivernum(), run.qid(), points), km));
      }
    }
    return kept;
  }

  /**
   * Drops stretches until the country has at most {@link #MAX_RIVER_POINTS} points: the highest
   * scalerank (least important) first, the shortest first among those.
   */
  static List<Kept> withinPointLimit(List<Kept> kept) {
    int points = kept.stream().mapToInt(k -> k.run().points().size()).sum();
    if (points <= MAX_RIVER_POINTS) {
      return kept;
    }
    List<Kept> byLeastImportant = new ArrayList<>(kept);
    byLeastImportant.sort(
        Comparator.comparingInt((Kept k) -> k.run().scalerank())
            .reversed()
            .thenComparingDouble(Kept::km)
            .thenComparingInt(k -> k.run().rivernum()));
    Set<Kept> dropped = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Kept k : byLeastImportant) {
      if (points <= MAX_RIVER_POINTS) {
        break;
      }
      dropped.add(k);
      points -= k.run().points().size();
    }
    return kept.stream().filter(k -> !dropped.contains(k)).toList();
  }

  static double km(List<Kept> kept) {
    return kept.stream().mapToDouble(Kept::km).sum();
  }
}
