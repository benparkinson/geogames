package com.parkinson.benjamin.geogames.loaders.rivers;

import static com.parkinson.benjamin.geogames.helper.CountryHelper.convertCountryNameToFileName;

import com.parkinson.benjamin.geogames.helper.CountryRiversEligibility;
import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.countries.CountryShapeBuilder;
import com.parkinson.benjamin.geogames.loaders.countries.CountryWriter;
import com.parkinson.benjamin.geogames.loaders.countries.ProcessedCountryReader;
import com.parkinson.benjamin.geogames.loaders.features.CountryLocator;
import com.parkinson.benjamin.geogames.loaders.features.DouglasPeucker;
import com.parkinson.benjamin.geogames.loaders.features.Geo;
import com.parkinson.benjamin.geogames.loaders.lakes.LakeGeometry;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverClipper.Run;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import tools.jackson.databind.JsonNode;

/**
 * Usage: CountryRiversMain &lt;rivers.geojson&gt; &lt;lakes.geojson&gt; &lt;countriesDir&gt;
 * &lt;outDir&gt;
 *
 * <p>Cuts every river of Natural Earth's rivers file at the country borders ({@link RiverClipper}),
 * simplifies each stretch, and writes one file per country with the rivers split into {@code major}
 * and {@code minor} by importance and the lakes on its shore, plus {@code index.json}, which lists
 * what is in each file. Offline and deterministic. Clears the {@code *.json} files in outDir first.
 *
 * <p>The app reads one country's file each time a round is built and holds none of them, so the
 * limits here ({@link #MAX_RIVER_POINTS}, {@link #MAX_LAKE_POINTS}) are what keep a round small.
 */
public class CountryRiversMain {

  /**
   * Rivers with a {@code scalerank} up to this are major: that is about a quarter of the total length
   * (the summary prints the distribution), which leaves the thick lines as the main clue and the
   * rest as texture.
   */
  static final int MAJOR_MAX_SCALERANK = 4;

  /** A stretch shorter than this (after simplifying) is a stub, not a river on this map. */
  static final double MIN_RUN_KM = 5;

  /** The most river points a country keeps: the least important rivers are dropped to fit. */
  static final int MAX_RIVER_POINTS = 4_000;

  /** The most lake points a country keeps: the smallest lakes are dropped to fit. */
  static final int MAX_LAKE_POINTS = 2_000;

  /** A lake needs this area to be a hint. */
  static final double MIN_LAKE_KM2 = 100;

  /** The shoreline a country needs on a lake to count as being on its shore. */
  static final double MIN_SHORE_KM = 5;

  static final String INDEX_FILE = "index.json";

  /** A stretch of river kept for a country, with its length. */
  record Kept(Run run, double km) {}

  /** A lake on a country's shore. */
  record Lake(double areaKm2, List<List<List<double[]>>> polygons) {}

  /**
   * What a run produced, for the summary.
   *
   * @param index what was written, sorted by name
   * @param rivers the files' contents, by country name
   * @param kmByScalerank the length of every river in the source, by scalerank
   * @param majorKm the length of the major rivers that were kept
   * @param minorKm the length of the other rivers that were kept
   * @param droppedForSize how many stretches were dropped to stay within {@link #MAX_RIVER_POINTS}
   * @param lakesDroppedForSize how many lakes were dropped to stay within {@link #MAX_LAKE_POINTS}
   * @param bytes the total size of the files written, with the index
   */
  public record Report(
      List<CountryRiversIndexEntry> index,
      Map<String, CountryRivers> rivers,
      Map<Integer, Double> kmByScalerank,
      double majorKm,
      double minorKm,
      int droppedForSize,
      int lakesDroppedForSize,
      long bytes) {}

  public static void main(String[] args) throws IOException {
    Report report = run(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
    printSummary(report);
  }

  /** Does everything {@code main} does except print. */
  static Report run(Path riversFile, Path lakesFile, Path countriesDir, Path outDir)
      throws IOException {
    List<Country> countries = new ProcessedCountryReader().readCountries(countriesDir);
    CountryLocator locator = new CountryLocator(countries, MIN_SHORE_KM);
    Map<String, Double> tolerances = new HashMap<>();
    for (Country country : countries) {
      if (!"Antarctica".equals(country.name())) {
        tolerances.put(country.name(), CountryShapeBuilder.tolerance(country));
      }
    }

    // Rivers, cut at the borders.
    RiverClipper clipper = new RiverClipper(locator);
    Map<String, List<Run>> runsByCountry = new TreeMap<>();
    Map<Integer, Double> kmByScalerank = new TreeMap<>();
    for (JsonNode feature : read(riversFile).path("features")) {
      JsonNode props = feature.path("properties");
      int scalerank = props.path("scalerank").asInt(99);
      int rivernum = props.path("rivernum").asInt(-1);
      for (List<double[]> line : lines(feature.path("geometry"))) {
        kmByScalerank.merge(scalerank, Geo.lineKm(line), Double::sum);
        clipper
            .clip(scalerank, rivernum, line)
            .forEach(
                (country, runs) ->
                    runsByCountry.computeIfAbsent(country, key -> new ArrayList<>()).addAll(runs));
      }
    }

    // Lakes, whole, given to every country on their shore.
    Map<String, List<Lake>> lakesByCountry = new TreeMap<>();
    for (JsonNode feature : read(lakesFile).path("features")) {
      List<List<List<double[]>>> raw = polygons(feature.path("geometry"));
      if (raw.isEmpty() || LakeGeometry.areaKm2(raw) < MIN_LAKE_KM2) {
        continue;
      }
      List<List<List<double[]>>> simplified = LakeGeometry.simplify(raw);
      if (simplified.isEmpty()) {
        continue;
      }
      Lake lake = new Lake(LakeGeometry.areaKm2(simplified), simplified);
      for (String country : locator.locateRings(simplified).countries()) {
        lakesByCountry.computeIfAbsent(country, key -> new ArrayList<>()).add(lake);
      }
    }

    // One file for each country with at least one stretch of river.
    Files.createDirectories(outDir);
    new CountryWriter().clearOutput(outDir);
    List<CountryRiversIndexEntry> index = new ArrayList<>();
    Map<String, CountryRivers> written = new TreeMap<>();
    double majorKm = 0;
    double minorKm = 0;
    int droppedForSize = 0;
    int lakesDroppedForSize = 0;
    long bytes = 0;
    for (Map.Entry<String, List<Run>> entry : runsByCountry.entrySet()) {
      String name = entry.getKey();
      List<Kept> kept = simplified(entry.getValue(), tolerances.get(name));
      if (kept.isEmpty()) {
        continue;
      }
      int before = kept.size();
      kept = withinPointLimit(kept);
      droppedForSize += before - kept.size();

      List<Lake> lakes =
          lakesByCountry.getOrDefault(name, List.of()).stream()
              .map(
                  lake ->
                      new Lake(lake.areaKm2(), simplifyLake(lake.polygons(), tolerances.get(name))))
              .toList();
      List<Lake> keptLakes = withinLakeLimit(lakes);
      lakesDroppedForSize += lakes.size() - keptLakes.size();

      List<Kept> major =
          kept.stream().filter(k -> k.run().scalerank() <= MAJOR_MAX_SCALERANK).toList();
      List<Kept> minor =
          kept.stream().filter(k -> k.run().scalerank() > MAJOR_MAX_SCALERANK).toList();
      majorKm += km(major);
      minorKm += km(minor);

      CountryRivers rivers =
          new CountryRivers(
              name, lineData(name, major), lineData(name, minor), lakeData(name, keptLakes));
      String file = convertCountryNameToFileName(name);
      Path path = outDir.resolve(file);
      Files.writeString(path, GeoJson.prettyWriter().writeValueAsString(rivers) + "\n");
      bytes += Files.size(path);
      written.put(name, rivers);
      index.add(
          new CountryRiversIndexEntry(
              name,
              file,
              (int) kept.stream().map(k -> k.run().rivernum()).distinct().count(),
              Math.round(km(kept)),
              keptLakes.size()));
    }
    index.sort(Comparator.comparing(CountryRiversIndexEntry::name));
    Path indexPath = outDir.resolve(INDEX_FILE);
    Files.writeString(indexPath, GeoJson.prettyWriter().writeValueAsString(index) + "\n");
    bytes += Files.size(indexPath);

    return new Report(
        index,
        written,
        kmByScalerank,
        majorKm,
        minorKm,
        droppedForSize,
        lakesDroppedForSize,
        bytes);
  }

  // ---- rivers ----

  /** Simplifies every run with the country's tolerance and drops the stubs. */
  private static List<Kept> simplified(List<Run> runs, double tolerance) {
    List<Kept> kept = new ArrayList<>();
    for (Run run : runs) {
      List<double[]> points = round(DouglasPeucker.simplify(run.points(), tolerance));
      if (points.size() < 2) {
        continue;
      }
      double km = Geo.lineKm(points);
      if (km >= MIN_RUN_KM) {
        kept.add(new Kept(new Run(run.scalerank(), run.rivernum(), points), km));
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

  private static double km(List<Kept> kept) {
    return kept.stream().mapToDouble(Kept::km).sum();
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

  // ---- lakes ----

  /**
   * Simplifies a lake's rings with the country's tolerance, so a lake on a large country's map isn't
   * drawn in more detail than that country's outline. A ring left with too few points is dropped (a
   * polygon with it).
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

  // ---- output ----

  private static GeoData lineData(String name, List<Kept> kept) {
    List<List<List<Double>>> lines = new ArrayList<>();
    for (Kept k : kept) {
      lines.add(toLine(k.run().points()));
    }
    return new GeoData(
        "Feature", new GeoProperties(name), new GeoGeometry("MultiLineString", lines));
  }

  private static GeoData lakeData(String name, List<Lake> lakes) {
    List<List<List<List<Double>>>> polygons = new ArrayList<>();
    for (Lake lake : lakes) {
      for (List<List<double[]>> polygon : lake.polygons()) {
        List<List<List<Double>>> rings = new ArrayList<>();
        for (List<double[]> ring : polygon) {
          rings.add(toLine(ring));
        }
        polygons.add(rings);
      }
    }
    return new GeoData(
        "Feature", new GeoProperties(name), new GeoGeometry("MultiPolygon", polygons));
  }

  private static List<List<Double>> toLine(List<double[]> points) {
    List<List<Double>> line = new ArrayList<>(points.size());
    for (double[] p : points) {
      line.add(List.of(p[0], p[1]));
    }
    return line;
  }

  // ---- reading ----

  private static JsonNode read(Path file) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return GeoJson.lenientReader().readTree(in);
    }
  }

  private static List<List<double[]>> lines(JsonNode geometry) {
    List<List<double[]>> lines = new ArrayList<>();
    String type = geometry.path("type").asString("");
    if (type.equals("LineString")) {
      lines.add(points(geometry.path("coordinates")));
    } else if (type.equals("MultiLineString")) {
      for (JsonNode line : geometry.path("coordinates")) {
        lines.add(points(line));
      }
    }
    return lines;
  }

  private static List<List<List<double[]>>> polygons(JsonNode geometry) {
    List<List<List<double[]>>> polygons = new ArrayList<>();
    String type = geometry.path("type").asString("");
    if (type.equals("Polygon")) {
      polygons.add(rings(geometry.path("coordinates")));
    } else if (type.equals("MultiPolygon")) {
      for (JsonNode polygon : geometry.path("coordinates")) {
        polygons.add(rings(polygon));
      }
    }
    return polygons;
  }

  private static List<List<double[]>> rings(JsonNode rings) {
    List<List<double[]>> result = new ArrayList<>();
    for (JsonNode ring : rings) {
      result.add(points(ring));
    }
    return result;
  }

  private static List<double[]> points(JsonNode line) {
    List<double[]> points = new ArrayList<>();
    for (JsonNode p : line) {
      points.add(new double[] {p.get(0).asDouble(), p.get(1).asDouble()});
    }
    return points;
  }

  // ---- summary ----

  private static void printSummary(Report report) {
    double total = report.kmByScalerank().values().stream().mapToDouble(Double::doubleValue).sum();
    System.out.println("Source river length by scalerank (lower is more important):");
    System.out.println("Major: scalerank up to " + MAJOR_MAX_SCALERANK);
    double cumulative = 0;
    for (Map.Entry<Integer, Double> e : report.kmByScalerank().entrySet()) {
      cumulative += e.getValue();
      System.out.printf(
          "  %2d: %8.0f km, %5.1f%% cumulative%n",
          e.getKey(), e.getValue(), 100 * cumulative / total);
    }
    double kept = report.majorKm() + report.minorKm();
    System.out.printf(
        "Kept: %.0f km, of which major %.1f%%%n",
        kept, kept == 0 ? 0 : 100 * report.majorKm() / kept);
    System.out.println(
        "Dropped to stay within "
            + MAX_RIVER_POINTS
            + " river points: "
            + report.droppedForSize()
            + " stretches; within "
            + MAX_LAKE_POINTS
            + " lake points: "
            + report.lakesDroppedForSize()
            + " lakes");

    List<CountryRiversIndexEntry> index = report.index();
    System.out.println("Written: " + index.size() + " countries, " + report.bytes() + " bytes");
    CountryRivers largest = null;
    int largestPoints = 0;
    for (CountryRivers rivers : report.rivers().values()) {
      int points =
          CountryShapeBuilder.pointCount(rivers.major())
              + CountryShapeBuilder.pointCount(rivers.minor());
      if (points > largestPoints) {
        largestPoints = points;
        largest = rivers;
      }
    }
    if (largest != null) {
      System.out.println("Most river points: " + largest.name() + " " + largestPoints);
    }

    List<CountryRiversIndexEntry> eligible =
        index.stream().filter(CountryRiversEligibility.countryRivers()).toList();
    System.out.println(
        "Eligible (at least "
            + CountryRiversEligibility.MIN_RIVERS
            + " rivers and "
            + (long) CountryRiversEligibility.MIN_RIVER_KM
            + " km): "
            + eligible.size());
    eligible.forEach(
        e ->
            System.out.println(
                "  "
                    + e.name()
                    + ": "
                    + e.riverCount()
                    + " rivers, "
                    + e.riverKm()
                    + " km, "
                    + e.lakeCount()
                    + " lakes"));
  }
}
