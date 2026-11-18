package com.parkinson.benjamin.geogames.loaders.countries;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A country's geometry as rings of vertices, with a key for each vertex and edge. */
final class CountryRings {

  /**
   * A closed ring without the repeated last point: edge i joins vertex i to vertex i+1 (wrapping).
   */
  record Ring(List<double[]> points, String[] keys) {
    int size() {
      return points.size();
    }
  }

  /** One polygon: the outer ring first, then the holes. */
  record Part(List<Ring> rings) {
    Ring outer() {
      return rings.get(0);
    }
  }

  record Source(String name, List<String> additionalNames, boolean polygon, List<Part> parts) {}

  private CountryRings() {}

  static Source read(Country country) {
    GeoGeometry geometry = country.geoData().geometry();
    List<?> coordinates = geometry.coordinates();
    boolean polygon = geometry.type().equals("Polygon");
    List<Part> parts = new ArrayList<>();
    if (polygon) {
      addPart(parts, coordinates);
    } else {
      for (Object part : coordinates) {
        addPart(parts, (List<?>) part);
      }
    }
    return new Source(country.name(), country.additionalNames(), polygon, parts);
  }

  private static void addPart(List<Part> parts, List<?> rings) {
    List<Ring> read = new ArrayList<>();
    for (Object ring : rings) {
      Ring r = toRing((List<?>) ring);
      if (r != null) {
        read.add(r);
      } else if (read.isEmpty()) {
        return; // an outer ring that is not a ring: skip the whole part
      }
    }
    if (!read.isEmpty()) {
      parts.add(new Part(read));
    }
  }

  /** Drops the closing point and repeated vertices. Null if what is left is not a ring. */
  private static Ring toRing(List<?> raw) {
    List<double[]> points = new ArrayList<>();
    List<String> keys = new ArrayList<>();
    for (Object o : raw) {
      List<?> p = (List<?>) o;
      double[] point = {((Number) p.get(0)).doubleValue(), ((Number) p.get(1)).doubleValue()};
      String key = vertexKey(point);
      if (keys.isEmpty() || !keys.get(keys.size() - 1).equals(key)) {
        points.add(point);
        keys.add(key);
      }
    }
    if (keys.size() > 1 && keys.get(0).equals(keys.get(keys.size() - 1))) {
      points.remove(points.size() - 1);
      keys.remove(keys.size() - 1);
    }
    if (points.size() < 3) {
      return null;
    }
    return new Ring(points, keys.toArray(new String[0]));
  }

  /** Coordinates to about 1 m: shared vertices are not bit-for-bit equal. */
  static String vertexKey(double[] p) {
    return format5(p[0]) + "," + format5(p[1]);
  }

  private static String format5(double v) {
    String s = String.format(Locale.ROOT, "%.5f", v);
    return s.equals("-0.00000") ? "0.00000" : s;
  }

  /** The same key for an edge whichever way round it is walked. */
  static String edgeKey(Ring ring, int i) {
    String a = ring.keys()[i];
    String b = ring.keys()[(i + 1) % ring.size()];
    return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
  }

  /** Where edge i ends. */
  static double[] end(Ring ring, int i) {
    return ring.points().get((i + 1) % ring.size());
  }

  /** Shoelace area in degrees: only good for comparing the parts of one country. */
  static double area(Ring ring) {
    double sum = 0;
    for (int i = 0; i < ring.size(); i++) {
      double[] a = ring.points().get(i);
      double[] b = end(ring, i);
      sum += a[0] * b[1] - b[0] * a[1];
    }
    return Math.abs(sum / 2);
  }
}
