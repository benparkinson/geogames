package com.parkinson.benjamin.geogames.loaders.countries;

import static com.parkinson.benjamin.geogames.loaders.countries.BorderRuns.COAST;
import static com.parkinson.benjamin.geogames.loaders.countries.BorderRuns.SEAM;
import static com.parkinson.benjamin.geogames.loaders.countries.CountryRings.area;
import static com.parkinson.benjamin.geogames.loaders.countries.CountryRings.edgeKey;
import static com.parkinson.benjamin.geogames.loaders.countries.CountryRings.end;
import static com.parkinson.benjamin.geogames.loaders.countries.CountryRings.vertexKey;

import com.parkinson.benjamin.geogames.loaders.countries.BorderRuns.Run;
import com.parkinson.benjamin.geogames.loaders.countries.CountryRings.Part;
import com.parkinson.benjamin.geogames.loaders.countries.CountryRings.Ring;
import com.parkinson.benjamin.geogames.loaders.countries.CountryRings.Source;
import com.parkinson.benjamin.geogames.loaders.features.Geo;
import com.parkinson.benjamin.geogames.loaders.features.GeoPoints;
import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.CountryShape.Neighbour;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns the full country geometry into small display shapes, and works out each country's coast,
 * land borders and neighbours from which edges two countries share.
 *
 * <p>Natural Earth comes from one topology, so where two countries meet they have the same
 * vertices. An edge used by one country is coast, and an edge used by two is a land border. Each
 * ring is split into runs of edges with the same label ({@link BorderRuns}) and every run is
 * simplified in a canonical direction ({@link SharedLineSimplifier}), so both countries get
 * identical points for a shared border and no gaps open up.
 */
public final class CountryShapeBuilder {

  static final double MIN_TOLERANCE = 0.002;
  static final double MAX_TOLERANCE = 0.02;
  static final double TOLERANCE_DIVISOR = 400;
  static final double TINY_ISLAND_KM = 10;

  private CountryShapeBuilder() {}

  /** What is worked out once for each country before any simplifying. */
  private record Info(int largest, Bounds focus, double tolerance, long areaKm2) {}

  /** The length of a country's coast and borders, from the edge labels. */
  private record Borders(
      List<List<String[]>> labels,
      double coastKm,
      double landKm,
      Map<String, Double> neighbourKm,
      Set<String> mainland) {}

  /** A country's drawn outline, coast and borders. */
  private record Drawn(
      GeoGeometry outline,
      List<List<List<Double>>> coastLines,
      List<List<List<Double>>> borderLines,
      Map<String, List<Integer>> neighbourLines) {}

  /**
   * The tolerance, in degrees, this builder simplifies a country's lines with: a four-hundredth of
   * the diagonal of its largest part, within limits. Other programs that draw things inside a
   * country (its rivers) simplify with the same tolerance, so they match the outline.
   */
  public static double tolerance(Country country) {
    return info(CountryRings.read(country)).tolerance();
  }

  public static List<CountryShape> build(List<Country> countries) {
    List<Source> sources =
        countries.stream()
            .filter(country -> !Geo.ANTARCTICA.equals(country.name()))
            .map(CountryRings::read)
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
    double areaKm2 = 0;
    List<Ring> rings = source.parts().get(largest).rings();
    for (int r = 0; r < rings.size(); r++) {
      double ringKm2 = Geo.ringAreaKm2(rings.get(r).points());
      areaKm2 += r == 0 ? ringKm2 : -ringKm2;
    }
    return new Info(largest, new Bounds(west, south, east, north), tolerance, Math.round(areaKm2));
  }

  // ---- one country ----

  private static CountryShape shape(
      Source source, Map<String, Set<String>> users, Map<String, Info> infos) {
    Info info = infos.get(source.name());
    Borders borders = measureBorders(source, info, users);
    Drawn drawn = drawParts(source, info, infos, borders.labels());
    return new CountryShape(
        source.name(),
        source.additionalNames(),
        Math.round(borders.coastKm()),
        Math.round(borders.landKm()),
        info.areaKm2(),
        neighbours(borders, drawn),
        info.focus(),
        GeoData.feature(source.name(), drawn.outline()),
        GeoData.feature(source.name(), new GeoGeometry("MultiLineString", drawn.coastLines())),
        GeoData.feature(source.name(), new GeoGeometry("MultiLineString", drawn.borderLines())));
  }

  /** Labels every edge, and adds up the coast and each neighbour's border. */
  private static Borders measureBorders(Source source, Info info, Map<String, Set<String>> users) {
    double coastKm = 0;
    double landKm = 0;
    Map<String, Double> neighbourKm = new HashMap<>();
    Set<String> mainland = new HashSet<>();

    List<List<String[]>> labelled = new ArrayList<>();
    for (int p = 0; p < source.parts().size(); p++) {
      List<String[]> partLabels = new ArrayList<>();
      for (Ring ring : source.parts().get(p).rings()) {
        String[] labels = BorderRuns.labels(source, ring, users);
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
    return new Borders(labelled, coastKm, landKm, neighbourKm, mainland);
  }

  /** Simplifies every run, collecting the coast, the borders and the outline they make up. */
  private static Drawn drawParts(
      Source source, Info info, Map<String, Info> infos, List<List<String[]>> labelled) {
    List<List<List<Double>>> coastLines = new ArrayList<>();
    List<List<List<Double>>> borderLines = new ArrayList<>();
    // The indexes into borderLines of the lines shared with each neighbour, ascending.
    Map<String, List<Integer>> neighbourLines = new HashMap<>();
    List<Object> outline = new ArrayList<>();
    for (int p = 0; p < source.parts().size(); p++) {
      Part part = source.parts().get(p);
      List<String[]> partLabels = labelled.get(p);

      List<Run> outerRuns = BorderRuns.runs(part.outer(), partLabels.get(0));
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
        List<Run> runs = r == 0 ? outerRuns : BorderRuns.runs(ring, partLabels.get(r));
        List<double[]> simplified = new ArrayList<>();
        for (Run run : runs) {
          List<double[]> points = simplify(run, infos, info);
          if (run.label().equals(COAST)) {
            coastLines.add(GeoPoints.toPositions(points));
          } else if (!run.label().equals(SEAM)) {
            neighbourLines
                .computeIfAbsent(run.label(), key -> new ArrayList<>())
                .add(borderLines.size());
            borderLines.add(GeoPoints.toPositions(points));
          }
          for (int j = simplified.isEmpty() ? 0 : 1; j < points.size(); j++) {
            simplified.add(points.get(j));
          }
        }
        if (simplified.size() < 4) {
          simplified = roundedRing(ring);
        }
        if (simplified.size() >= 4) {
          rings.add(GeoPoints.toPositions(simplified));
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
    return new Drawn(outlineGeometry, coastLines, borderLines, neighbourLines);
  }

  /** The neighbours, longest border first, then by name. */
  private static List<Neighbour> neighbours(Borders borders, Drawn drawn) {
    return borders.neighbourKm().entrySet().stream()
        .map(
            e ->
                new Neighbour(
                    e.getKey(),
                    Math.round(e.getValue()),
                    borders.mainland().contains(e.getKey()),
                    drawn.neighbourLines().getOrDefault(e.getKey(), List.of())))
        .sorted(Comparator.comparingDouble(Neighbour::km).reversed().thenComparing(Neighbour::name))
        .toList();
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
    return GeoPoints.roundLine(points);
  }

  // ---- simplifying ----

  private static List<double[]> simplify(Run run, Map<String, Info> infos, Info own) {
    double tolerance = own.tolerance();
    if (!run.label().equals(COAST) && !run.label().equals(SEAM)) {
      tolerance = Math.min(tolerance, infos.get(run.label()).tolerance());
    }
    List<double[]> points = run.points();
    boolean closed = vertexKey(points.get(0)).equals(vertexKey(points.get(points.size() - 1)));
    return closed
        ? SharedLineSimplifier.simplifyClosed(points, tolerance)
        : SharedLineSimplifier.simplifyOpen(points, tolerance);
  }
}
