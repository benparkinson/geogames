package com.parkinson.benjamin.geogames.loaders.countries;

import com.parkinson.benjamin.geogames.loaders.features.DouglasPeucker;
import com.parkinson.benjamin.geogames.loaders.features.Geo;
import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShape.Neighbour;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns the full country geometry into small display shapes, and works out each country's coast,
 * land borders and neighbours from which edges two countries share.
 *
 * <p>Natural Earth comes from one topology, so where two countries meet they have the same
 * vertices. An edge used by one country is coast, and an edge used by two is a land border. Each
 * ring is split into runs of edges with the same label and every run is simplified in a canonical
 * direction, so both countries get identical points for a shared border and no gaps open up.
 */
public final class CountryShapeBuilder {

  static final String ANTARCTICA = "Antarctica";
  static final double SEAM_LON = 179.9999;
  static final double MIN_TOLERANCE = 0.002;
  static final double MAX_TOLERANCE = 0.02;
  static final double TOLERANCE_DIVISOR = 400;
  static final double TINY_ISLAND_KM = 10;

  private static final String COAST = "\u0000coast";
  private static final String SEAM = "\u0000seam";

  private CountryShapeBuilder() {}

  /** A closed ring without the repeated last point: edge i joins vertex i to vertex i+1 (wrapping). */
  private record Ring(List<double[]> points, String[] keys) {
    int size() {
      return points.size();
    }
  }

  /** One polygon: the outer ring first, then the holes. */
  private record Part(List<Ring> rings) {
    Ring outer() {
      return rings.get(0);
    }
  }

  private record Source(
      String name, List<String> additionalNames, boolean polygon, List<Part> parts) {}

  private record Run(String label, List<double[]> points) {}

  /** What is worked out once for each country before any simplifying. */
  private record Info(int largest, Bounds focus, double tolerance) {}

  public static List<CountryShape> build(List<Country> countries) {
    List<Source> sources =
        countries.stream()
            .filter(country -> !ANTARCTICA.equals(country.name()))
            .map(CountryShapeBuilder::read)
            .toList();

    // Which countries use each edge.
    Map<String, Set<String>> users = new HashMap<>();
    for (Source source : sources) {
      for (Part part : source.parts()) {
        for (Ring ring : part.rings()) {
          for (int i = 0; i < ring.size(); i++) {
            users.computeIfAbsent(edgeKey(ring, i), key -> new TreeSet<>()).add(source.name());
          }
        }
      }
    }

    Map<String, Info> infos = new HashMap<>();
    for (Source source : sources) {
      infos.put(source.name(), info(source));
    }

    List<CountryShape> shapes = new ArrayList<>();
    for (Source source : sources) {
      shapes.add(shape(source, users, infos));
    }
    return shapes;
  }

  // ---- reading ----

  private static Source read(Country country) {
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

  private static String edgeKey(Ring ring, int i) {
    String a = ring.keys()[i];
    String b = ring.keys()[(i + 1) % ring.size()];
    return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
  }

  private static double[] end(Ring ring, int i) {
    return ring.points().get((i + 1) % ring.size());
  }

  // ---- per-country facts ----

  private static Info info(Source source) {
    int largest = 0;
    double largestArea = -1;
    for (int i = 0; i < source.parts().size(); i++) {
      double area = area(source.parts().get(i).outer());
      if (area > largestArea) {
        largestArea = area;
        largest = i;
      }
    }
    double west = Double.MAX_VALUE;
    double south = Double.MAX_VALUE;
    double east = -Double.MAX_VALUE;
    double north = -Double.MAX_VALUE;
    for (double[] p : source.parts().get(largest).outer().points()) {
      west = Math.min(west, p[0]);
      east = Math.max(east, p[0]);
      south = Math.min(south, p[1]);
      north = Math.max(north, p[1]);
    }
    double diagonal = Math.hypot(east - west, north - south);
    double tolerance =
        Math.max(MIN_TOLERANCE, Math.min(MAX_TOLERANCE, diagonal / TOLERANCE_DIVISOR));
    return new Info(largest, new Bounds(west, south, east, north), tolerance);
  }

  /** Shoelace area in degrees: only good for comparing the parts of one country. */
  private static double area(Ring ring) {
    double sum = 0;
    for (int i = 0; i < ring.size(); i++) {
      double[] a = ring.points().get(i);
      double[] b = end(ring, i);
      sum += a[0] * b[1] - b[0] * a[1];
    }
    return Math.abs(sum / 2);
  }

  private static String[] labels(Source source, Ring ring, Map<String, Set<String>> users) {
    String[] labels = new String[ring.size()];
    for (int i = 0; i < labels.length; i++) {
      double[] a = ring.points().get(i);
      double[] b = end(ring, i);
      if (Math.abs(a[0]) >= SEAM_LON && Math.abs(b[0]) >= SEAM_LON && a[0] * b[0] > 0) {
        labels[i] = SEAM;
        continue;
      }
      Set<String> using = users.get(edgeKey(ring, i));
      if (using.size() > 2) {
        throw new IllegalStateException(
            "More than two countries share an edge near %s: %s"
                .formatted(vertexKey(a), String.join(", ", using)));
      }
      labels[i] =
          using.size() == 2
              ? using.stream().filter(name -> !name.equals(source.name())).findFirst().orElse(COAST)
              : COAST;
    }
    return labels;
  }

  // ---- one country ----

  private static CountryShape shape(
      Source source, Map<String, Set<String>> users, Map<String, Info> infos) {
    Info info = infos.get(source.name());
    double coastKm = 0;
    double landKm = 0;
    Map<String, Double> neighbourKm = new HashMap<>();
    Set<String> mainland = new HashSet<>();

    List<List<String[]>> labelled = new ArrayList<>();
    for (int p = 0; p < source.parts().size(); p++) {
      List<String[]> partLabels = new ArrayList<>();
      for (Ring ring : source.parts().get(p).rings()) {
        String[] labels = labels(source, ring, users);
        partLabels.add(labels);
        for (int i = 0; i < labels.length; i++) {
          if (labels[i].equals(SEAM)) {
            continue;
          }
          double km = Geo.haversineKm(ring.points().get(i), end(ring, i));
          if (labels[i].equals(COAST)) {
            coastKm += km;
          } else {
            landKm += km;
            neighbourKm.merge(labels[i], km, Double::sum);
            if (p == info.largest()) {
              mainland.add(labels[i]);
            }
          }
        }
      }
      labelled.add(partLabels);
    }

    List<List<List<Double>>> coastLines = new ArrayList<>();
    List<List<List<Double>>> borderLines = new ArrayList<>();
    List<Object> outline = new ArrayList<>();
    for (int p = 0; p < source.parts().size(); p++) {
      Part part = source.parts().get(p);
      List<String[]> partLabels = labelled.get(p);

      List<Run> outerRuns = runs(part.outer(), partLabels.get(0));
      boolean tinyIsland =
          p != info.largest()
              && outerRuns.size() == 1
              && outerRuns.get(0).label().equals(COAST)
              && perimeterKm(part.outer()) < TINY_ISLAND_KM;
      if (tinyIsland) {
        continue;
      }

      List<List<List<Double>>> rings = new ArrayList<>();
      for (int r = 0; r < part.rings().size(); r++) {
        Ring ring = part.rings().get(r);
        List<Run> runs = r == 0 ? outerRuns : runs(ring, partLabels.get(r));
        List<double[]> simplified = new ArrayList<>();
        for (Run run : runs) {
          List<double[]> points = simplify(run, infos, info);
          if (run.label().equals(COAST)) {
            coastLines.add(toLine(points));
          } else if (!run.label().equals(SEAM)) {
            borderLines.add(toLine(points));
          }
          for (int j = simplified.isEmpty() ? 0 : 1; j < points.size(); j++) {
            simplified.add(points.get(j));
          }
        }
        if (simplified.size() < 4) {
          simplified = roundedRing(ring);
        }
        if (simplified.size() >= 4) {
          rings.add(toLine(simplified));
        }
      }
      if (!rings.isEmpty()) {
        outline.add(rings);
      }
    }

    GeoGeometry outlineGeometry =
        source.polygon() && outline.size() == 1
            ? new GeoGeometry("Polygon", (List<?>) outline.get(0))
            : new GeoGeometry("MultiPolygon", outline);

    List<Neighbour> neighbours =
        neighbourKm.entrySet().stream()
            .map(
                e ->
                    new Neighbour(
                        e.getKey(), Math.round(e.getValue()), mainland.contains(e.getKey())))
            .sorted(
                Comparator.comparingDouble(Neighbour::km).reversed().thenComparing(Neighbour::name))
            .toList();

    return new CountryShape(
        source.name(),
        source.additionalNames(),
        Math.round(coastKm),
        Math.round(landKm),
        neighbours,
        info.focus(),
        feature(source.name(), outlineGeometry),
        feature(source.name(), new GeoGeometry("MultiLineString", coastLines)),
        feature(source.name(), new GeoGeometry("MultiLineString", borderLines)));
  }

  private static GeoData feature(String name, GeoGeometry geometry) {
    return new GeoData("Feature", new GeoProperties(name), geometry);
  }

  private static List<List<Double>> toLine(List<double[]> points) {
    List<List<Double>> line = new ArrayList<>(points.size());
    for (double[] p : points) {
      line.add(List.of(p[0], p[1]));
    }
    return line;
  }

  private static double perimeterKm(Ring ring) {
    double km = 0;
    for (int i = 0; i < ring.size(); i++) {
      km += Geo.haversineKm(ring.points().get(i), end(ring, i));
    }
    return km;
  }

  private static List<double[]> roundedRing(Ring ring) {
    List<double[]> points = new ArrayList<>(ring.points());
    points.add(ring.points().get(0));
    return round(points);
  }

  // ---- runs ----

  /**
   * Splits a ring into maximal runs of edges with the same label, starting at a label change. A
   * ring with one label throughout is one closed run (first point equals last).
   */
  private static List<Run> runs(Ring ring, String[] labels) {
    int m = ring.size();
    int start = -1;
    for (int i = 0; i < m; i++) {
      if (!labels[i].equals(labels[(i + m - 1) % m])) {
        start = i;
        break;
      }
    }
    if (start < 0) {
      List<double[]> closed = new ArrayList<>(ring.points());
      closed.add(ring.points().get(0));
      return List.of(new Run(labels[0], closed));
    }
    List<Run> runs = new ArrayList<>();
    String label = null;
    List<double[]> points = null;
    for (int k = 0; k < m; k++) {
      int edge = (start + k) % m;
      if (!labels[edge].equals(label)) {
        if (points != null) {
          runs.add(new Run(label, points));
        }
        label = labels[edge];
        points = new ArrayList<>();
        points.add(ring.points().get(edge));
      }
      points.add(end(ring, edge));
    }
    runs.add(new Run(label, points));
    return runs;
  }

  // ---- simplifying ----

  private static List<double[]> simplify(Run run, Map<String, Info> infos, Info own) {
    double tolerance = own.tolerance();
    if (!run.label().equals(COAST) && !run.label().equals(SEAM)) {
      tolerance = Math.min(tolerance, infos.get(run.label()).tolerance());
    }
    List<double[]> points = run.points();
    boolean closed = vertexKey(points.get(0)).equals(vertexKey(points.get(points.size() - 1)));
    return closed ? simplifyClosed(points, tolerance) : simplifyOpen(points, tolerance);
  }

  /** Simplifies in the direction where the first vertex key is not greater than the last. */
  static List<double[]> simplifyOpen(List<double[]> points, double tolerance) {
    boolean reverse =
        vertexKey(points.get(0)).compareTo(vertexKey(points.get(points.size() - 1))) > 0;
    List<double[]> input = new ArrayList<>(points);
    if (reverse) {
      Collections.reverse(input);
    }
    List<double[]> result = DouglasPeucker.simplify(input, tolerance);
    if (reverse) {
      Collections.reverse(result);
    }
    return round(result);
  }

  /**
   * A closed run has no ends to anchor on, so it starts at its smallest vertex key, runs in the
   * direction whose second key is the smaller, and is cut at the vertex furthest from the start.
   * Whichever country calls this, the same ring gives the same points.
   */
  static List<double[]> simplifyClosed(List<double[]> closed, double tolerance) {
    int m = closed.size() - 1;
    int smallest = 0;
    for (int i = 1; i < m; i++) {
      if (vertexKey(closed.get(i)).compareTo(vertexKey(closed.get(smallest))) < 0) {
        smallest = i;
      }
    }
    List<double[]> ring = new ArrayList<>();
    for (int i = 0; i <= m; i++) {
      ring.add(closed.get((smallest + i) % m));
    }
    boolean reversed = vertexKey(ring.get(1)).compareTo(vertexKey(ring.get(m - 1))) > 0;
    if (reversed) {
      Collections.reverse(ring);
    }

    int far = 1;
    double farDistance = -1;
    for (int i = 1; i < m; i++) {
      double d = Math.hypot(ring.get(i)[0] - ring.get(0)[0], ring.get(i)[1] - ring.get(0)[1]);
      if (d > farDistance) {
        farDistance = d;
        far = i;
      }
    }
    List<double[]> joined =
        new ArrayList<>(DouglasPeucker.simplify(ring.subList(0, far + 1), tolerance));
    List<double[]> second = DouglasPeucker.simplify(ring.subList(far, m + 1), tolerance);
    joined.addAll(second.subList(1, second.size()));
    if (joined.size() < 4) {
      joined = ring;
    }
    if (reversed) {
      joined = new ArrayList<>(joined);
      Collections.reverse(joined);
    }
    return round(joined);
  }

  private static List<double[]> round(List<double[]> points) {
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

  private static double round4(double v) {
    return Math.round(v * 10_000d) / 10_000d;
  }

  /** Every position in a Polygon, MultiPolygon or MultiLineString. */
  public static int pointCount(GeoData data) {
    return countPositions(data.geometry().coordinates());
  }

  private static int countPositions(List<?> list) {
    if (list.isEmpty()) {
      return 0;
    }
    if (list.get(0) instanceof Number) {
      return 1;
    }
    int total = 0;
    for (Object o : list) {
      total += countPositions((List<?>) o);
    }
    return total;
  }
}
