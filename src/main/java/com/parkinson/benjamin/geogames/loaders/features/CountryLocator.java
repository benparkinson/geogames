package com.parkinson.benjamin.geogames.loaders.features;

import com.parkinson.benjamin.geogames.model.Country;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out which countries a feature (a river, or a lake shoreline) lies in by sampling points along its lines every couple
 * of kilometres and testing them against the country polygons.
 */
public class CountryLocator {

  static final double SAMPLE_KM = 2;
  private static final String SKIPPED = "Antarctica";

  /**
   * @param countries names sorted by km inside the country (longest first, ties by name), keeping
   *     only the countries with at least the minimum km
   * @param unlocatedKm the km of line that fell in no country at all
   */
  public record Result(List<String> countries, double unlocatedKm) {}

  /** One polygon: an outer ring and holes, with a bounding box. */
  private record Part(
      double minLon,
      double minLat,
      double maxLon,
      double maxLat,
      double[][] outer,
      List<double[][]> holes) {

    boolean contains(double lon, double lat) {
      if (lon < minLon || lon > maxLon || lat < minLat || lat > maxLat) {
        return false;
      }
      if (!inRing(outer, lon, lat)) {
        return false;
      }
      for (double[][] hole : holes) {
        if (inRing(hole, lon, lat)) {
          return false;
        }
      }
      return true;
    }
  }

  private record Shape(String name, List<Part> parts) {}

  private final List<Shape> shapes = new ArrayList<>();
  private final double minKm;

  /** @param minKm the km a country needs to be listed (10 for rivers, 5 for lakes) */
  public CountryLocator(List<Country> countries, double minKm) {
    this.minKm = minKm;
    for (Country country : countries) {
      if (SKIPPED.equals(country.name())) {
        continue;
      }
      List<Part> parts = new ArrayList<>();
      String type = country.geoData().geometry().type();
      List<?> coordinates = country.geoData().geometry().coordinates();
      if (type.equals("Polygon")) {
        parts.add(toPart(coordinates));
      } else if (type.equals("MultiPolygon")) {
        for (Object polygon : coordinates) {
          parts.add(toPart((List<?>) polygon));
        }
      }
      shapes.add(new Shape(country.name(), parts));
    }
  }

  public Result locate(List<List<double[]>> lines) {
    Map<String, Double> kmByCountry = new LinkedHashMap<>();
    double unlocated = 0;
    for (List<double[]> line : lines) {
      for (int i = 1; i < line.size(); i++) {
        double[] a = line.get(i - 1);
        double[] b = line.get(i);
        double km = Geo.haversineKm(a, b);
        int samples = Math.max(1, (int) Math.ceil(km / SAMPLE_KM));
        double each = km / samples;
        for (int s = 0; s < samples; s++) {
          double t = (s + 0.5) / samples;
          String country = countryAt(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t);
          if (country == null) {
            unlocated += each;
          } else {
            kmByCountry.merge(country, each, Double::sum);
          }
        }
      }
    }

    List<Map.Entry<String, Double>> kept =
        new ArrayList<>(
            kmByCountry.entrySet().stream().filter(e -> e.getValue() >= minKm).toList());
    kept.sort(
        Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue)
            .reversed()
            .thenComparing(Map.Entry::getKey));
    return new Result(kept.stream().map(Map.Entry::getKey).toList(), unlocated);
  }

  /**
   * Locates the outer ring of each polygon, treated as a closed line. Holes (islands in a lake) are
   * ignored.
   */
  public Result locateRings(List<List<List<double[]>>> polygons) {
    return locate(polygons.stream().map(polygon -> polygon.get(0)).toList());
  }

  private String countryAt(double lon, double lat) {
    for (Shape shape : shapes) {
      for (Part part : shape.parts()) {
        if (part.contains(lon, lat)) {
          return shape.name();
        }
      }
    }
    return null;
  }

  private static Part toPart(List<?> rings) {
    double[][] outer = toRing((List<?>) rings.get(0));
    List<double[][]> holes = new ArrayList<>();
    for (int i = 1; i < rings.size(); i++) {
      holes.add(toRing((List<?>) rings.get(i)));
    }
    double minLon = Double.MAX_VALUE;
    double minLat = Double.MAX_VALUE;
    double maxLon = -Double.MAX_VALUE;
    double maxLat = -Double.MAX_VALUE;
    for (double[] p : outer) {
      minLon = Math.min(minLon, p[0]);
      maxLon = Math.max(maxLon, p[0]);
      minLat = Math.min(minLat, p[1]);
      maxLat = Math.max(maxLat, p[1]);
    }
    return new Part(minLon, minLat, maxLon, maxLat, outer, holes);
  }

  private static double[][] toRing(List<?> points) {
    double[][] ring = new double[points.size()][];
    for (int i = 0; i < ring.length; i++) {
      List<?> p = (List<?>) points.get(i);
      ring[i] = new double[] {((Number) p.get(0)).doubleValue(), ((Number) p.get(1)).doubleValue()};
    }
    return ring;
  }

  /** Even-odd ray casting. */
  private static boolean inRing(double[][] ring, double lon, double lat) {
    boolean inside = false;
    for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
      double xi = ring[i][0];
      double yi = ring[i][1];
      double xj = ring[j][0];
      double yj = ring[j][1];
      if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) {
        inside = !inside;
      }
    }
    return inside;
  }
}
