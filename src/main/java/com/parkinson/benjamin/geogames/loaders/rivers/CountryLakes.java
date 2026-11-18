package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.loaders.features.DouglasPeucker;
import com.parkinson.benjamin.geogames.loaders.features.GeoPoints;
import com.parkinson.benjamin.geogames.loaders.lakes.LakeGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The lakes on a country's shore: simplifying them, the size limit and the output geometry. */
final class CountryLakes {

  /** The most lake points a country keeps: the smallest lakes are dropped to fit. */
  static final int MAX_LAKE_POINTS = 2_000;

  /** A lake needs this area to be a hint. */
  static final double MIN_LAKE_KM2 = 100;

  /** A lake on a country's shore. */
  record Lake(double areaKm2, List<List<List<double[]>>> polygons) {}

  private CountryLakes() {}

  /**
   * Simplifies a lake's rings with the country's tolerance, so a lake on a large country's map
   * isn't drawn in more detail than that country's outline. A ring left with too few points is
   * dropped (a polygon with it).
   */
  static List<List<List<double[]>>> simplifyLake(
      List<List<List<double[]>>> polygons, double tolerance) {
    List<List<List<double[]>>> result = new ArrayList<>();
    for (List<List<double[]>> polygon : polygons) {
      List<List<double[]>> rings = new ArrayList<>();
      for (int i = 0; i < polygon.size(); i++) {
        List<double[]> ring = DouglasPeucker.simplify(polygon.get(i), tolerance);
        if (ring.size() >= 4) {
          rings.add(ring);
        } else if (i == 0) {
          break; // without its outer ring the whole polygon goes
        }
      }
      if (!rings.isEmpty()) {
        result.add(rings);
      }
    }
    return result;
  }

  /** The largest lakes that fit in {@link #MAX_LAKE_POINTS} points, the largest first. */
  static List<Lake> withinLakeLimit(List<Lake> lakes) {
    List<Lake> bySize = new ArrayList<>(lakes);
    bySize.sort(Comparator.comparingDouble(Lake::areaKm2).reversed());
    List<Lake> kept = new ArrayList<>();
    int points = 0;
    for (Lake lake : bySize) {
      int size = LakeGeometry.pointCount(lake.polygons());
      if (points + size <= MAX_LAKE_POINTS) {
        kept.add(lake);
        points += size;
      }
    }
    return kept;
  }

  static GeoData lakeData(String name, List<Lake> lakes) {
    List<List<List<List<Double>>>> polygons = new ArrayList<>();
    for (Lake lake : lakes) {
      for (List<List<double[]>> polygon : lake.polygons()) {
        List<List<List<Double>>> rings = new ArrayList<>();
        for (List<double[]> ring : polygon) {
          rings.add(GeoPoints.toPositions(ring));
        }
        polygons.add(rings);
      }
    }
    return GeoData.feature(name, new GeoGeometry("MultiPolygon", polygons));
  }
}
