package com.parkinson.benjamin.geogames.loaders.rivers;

import static com.parkinson.benjamin.geogames.loaders.features.CountryHelper.convertCountryNameToFileName;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.countries.CountryShapeBuilder;
import com.parkinson.benjamin.geogames.loaders.countries.CountryWriter;
import com.parkinson.benjamin.geogames.loaders.countries.ProcessedCountryReader;
import com.parkinson.benjamin.geogames.loaders.features.CountryLocator;
import com.parkinson.benjamin.geogames.loaders.features.Geo;
import com.parkinson.benjamin.geogames.loaders.features.GeoJsonCoordinates;
import com.parkinson.benjamin.geogames.loaders.features.GeoPoints;
import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthFeature;
import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthReader;
import com.parkinson.benjamin.geogames.loaders.features.WikidataEntry;
import com.parkinson.benjamin.geogames.loaders.lakes.LakeGeometry;
import com.parkinson.benjamin.geogames.loaders.rivers.CountryLakes.Lake;
import com.parkinson.benjamin.geogames.loaders.rivers.CountryRiverLimits.Kept;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverClipper.Run;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

/**
 * Usage: CountryRiversMain &lt;rivers.geojson&gt; &lt;wikidata.json&gt; &lt;overrides.json&gt;
 * &lt;lakes.geojson&gt; &lt;countriesDir&gt; &lt;outDir&gt;
 *
 * <p>Cuts every river of Natural Earth's rivers file at the country borders ({@link RiverClipper}),
 * simplifies each stretch, and writes one file per country with the rivers split into {@code major}
 * and {@code minor} by importance, the lakes on its shore and the name of its longest named river
 * ({@link CountryLongestRiver}), plus {@code index.json}, which lists what is in each file. The
 * names come from {@link RiverAssembler} run over the whole file, with the same names, aliases and
 * overrides as the river games but without {@link RiverLoaderMain}'s continent quotas. Offline and
 * deterministic. Clears the {@code *.json} files in outDir first.
 *
 * <p>The app reads one country's file each time a round is built and holds none of them, so the
 * limits ({@link CountryRiverLimits#MAX_RIVER_POINTS}, {@link CountryLakes#MAX_LAKE_POINTS}) are
 * what keep a round small.
 */
public class CountryRiversMain {

  /**
   * Rivers with a {@code scalerank} up to this are major: that is about a quarter of the total
   * length (the summary prints the distribution), which leaves the thick lines as the main clue and
   * the rest as texture.
   */
  static final int MAJOR_MAX_SCALERANK = 4;

  /** The shoreline a country needs on a lake to count as being on its shore. */
  static final double MIN_SHORE_KM = 5;

  static final String INDEX_FILE = "index.json";

  /**
   * What a run produced, for the summary.
   *
   * @param index what was written, sorted by name
   * @param rivers the files' contents, by country name
   * @param kmByScalerank the length of every river in the source, by scalerank
   * @param majorKm the length of the major rivers that were kept
   * @param minorKm the length of the other rivers that were kept
   * @param droppedForSize how many stretches were dropped to stay within {@link
   *     CountryRiverLimits#MAX_RIVER_POINTS}
   * @param lakesDroppedForSize how many lakes were dropped to stay within {@link
   *     CountryLakes#MAX_LAKE_POINTS}
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

  /** The river stretches in each country, and the source's length by scalerank. */
  private record Clipped(
      Map<String, List<Run>> runsByCountry, Map<Integer, Double> kmByScalerank) {}

  /** One country's file as written, with what the summary needs to know about it. */
  private record Written(
      CountryRivers rivers,
      CountryRiversIndexEntry entry,
      double majorKm,
      double minorKm,
      int droppedForSize,
      int lakesDroppedForSize,
      long bytes) {}

  public static void main(String[] args) throws IOException {
    Report report =
        run(
            Path.of(args[0]),
            Path.of(args[1]),
            Path.of(args[2]),
            Path.of(args[3]),
            Path.of(args[4]),
            Path.of(args[5]));
    CountryRiversSummary.printSummary(report);
  }

  /** Does everything {@code main} does except print. */
  static Report run(
      Path riversFile,
      Path wikidataFile,
      Path overridesFile,
      Path lakesFile,
      Path countriesDir,
      Path outDir)
      throws IOException {
    List<Country> countries = new ProcessedCountryReader().readCountries(countriesDir);
    Map<String, List<String>> namesByCountry = new HashMap<>();
    for (Country country : countries) {
      List<String> names = new ArrayList<>();
      names.add(country.name());
      names.addAll(country.additionalNames());
      namesByCountry.put(country.name(), names);
    }
    Map<String, RiverCandidate> candidates = namedRivers(riversFile, wikidataFile, overridesFile);
    CountryLocator locator = new CountryLocator(countries, MIN_SHORE_KM);
    Map<String, Double> tolerances = new HashMap<>();
    for (Country country : countries) {
      if (!Geo.ANTARCTICA.equals(country.name())) {
        tolerances.put(country.name(), CountryShapeBuilder.tolerance(country));
      }
    }

    Clipped clipped = clipRivers(riversFile, overridesFile, locator);
    Map<String, List<Lake>> lakesByCountry = collectLakes(lakesFile, locator);

    // One file for each country with at least one stretch of river.
    Files.createDirectories(outDir);
    new CountryWriter().clearOutput(outDir);
    List<CountryRiversIndexEntry> index = new ArrayList<>();
    Map<String, CountryRivers> rivers = new TreeMap<>();
    double majorKm = 0;
    double minorKm = 0;
    int droppedForSize = 0;
    int lakesDroppedForSize = 0;
    long bytes = 0;
    for (Map.Entry<String, List<Run>> entry : clipped.runsByCountry().entrySet()) {
      String name = entry.getKey();
      Optional<Written> written =
          writeCountry(
              name,
              entry.getValue(),
              lakesByCountry.getOrDefault(name, List.of()),
              tolerances.get(name),
              CountryLongestRiver.of(
                      namesByCountry.getOrDefault(name, List.of(name)),
                      entry.getValue(),
                      candidates)
                  .orElse(null),
              outDir);
      if (written.isEmpty()) {
        continue;
      }
      Written w = written.get();
      rivers.put(name, w.rivers());
      index.add(w.entry());
      majorKm += w.majorKm();
      minorKm += w.minorKm();
      droppedForSize += w.droppedForSize();
      lakesDroppedForSize += w.lakesDroppedForSize();
      bytes += w.bytes();
    }
    index.sort(Comparator.comparing(CountryRiversIndexEntry::name));
    Path indexPath = outDir.resolve(INDEX_FILE);
    Files.writeString(indexPath, GeoJson.prettyWriter().writeValueAsString(index) + "\n");
    bytes += Files.size(indexPath);

    return new Report(
        index,
        rivers,
        clipped.kmByScalerank(),
        majorKm,
        minorKm,
        droppedForSize,
        lakesDroppedForSize,
        bytes);
  }

  /**
   * Every river with a Wikidata ID, a name and at least {@link RiverAssembler#MIN_RIVER_KM}, by
   * Wikidata ID. Built as {@link RiverLoaderMain} builds its candidates, but without picking the
   * most famous: the 50-river limit belongs to the river games, whose files are all cached.
   */
  private static Map<String, RiverCandidate> namedRivers(
      Path riversFile, Path wikidataFile, Path overridesFile) throws IOException {
    Map<String, RiverOverride> overrides =
        GeoJson.readValue(overridesFile, new TypeReference<>() {});
    JsonNode tree = GeoJson.readTree(riversFile);
    RiverQids.correct(tree, overrides);
    Map<String, NaturalEarthFeature> groups = new NaturalEarthReader().group(tree, Set.of("Canal"));
    Map<String, WikidataEntry> wikidata = GeoJson.readValue(wikidataFile, new TypeReference<>() {});
    Map<String, RiverCandidate> candidates = new HashMap<>();
    for (RiverCandidate candidate :
        new RiverAssembler().assemble(groups, wikidata, overrides).candidates()) {
      candidates.put(candidate.qid(), candidate);
    }
    return candidates;
  }

  /** Cuts every river of the source at the country borders. */
  private static Clipped clipRivers(Path riversFile, Path overridesFile, CountryLocator locator)
      throws IOException {
    Map<String, RiverOverride> overrides =
        GeoJson.readValue(overridesFile, new TypeReference<>() {});
    JsonNode tree = GeoJson.readTree(riversFile);
    RiverQids.correct(tree, overrides);
    RiverClipper clipper = new RiverClipper(locator);
    Map<String, List<Run>> runsByCountry = new TreeMap<>();
    Map<Integer, Double> kmByScalerank = new TreeMap<>();
    for (JsonNode feature : tree.path("features")) {
      JsonNode props = feature.path("properties");
      int scalerank = props.path("scalerank").asInt(99);
      int rivernum = props.path("rivernum").asInt(-1);
      String wikidataId = NaturalEarthReader.wikidataId(props);
      String qid = wikidataId.isBlank() ? null : wikidataId;
      for (List<double[]> line : GeoJsonCoordinates.lines(feature.path("geometry"))) {
        kmByScalerank.merge(scalerank, Geo.lineKm(line), Double::sum);
        clipper
            .clip(scalerank, rivernum, qid, line)
            .forEach(
                (country, runs) ->
                    runsByCountry.computeIfAbsent(country, key -> new ArrayList<>()).addAll(runs));
      }
    }
    return new Clipped(runsByCountry, kmByScalerank);
  }

  /** The big lakes, whole, given to every country on their shore. */
  private static Map<String, List<Lake>> collectLakes(Path lakesFile, CountryLocator locator)
      throws IOException {
    Map<String, List<Lake>> lakesByCountry = new TreeMap<>();
    for (JsonNode feature : GeoJson.readTree(lakesFile).path("features")) {
      List<List<List<double[]>>> raw = GeoJsonCoordinates.polygons(feature.path("geometry"));
      if (raw.isEmpty() || LakeGeometry.areaKm2(raw) < CountryLakes.MIN_LAKE_KM2) {
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
    return lakesByCountry;
  }

  /**
   * Writes one country's file, or nothing when no stretch of river is left after simplifying.
   * {@code longestRiver} is the name of its longest named river, or null.
   */
  private static Optional<Written> writeCountry(
      String name,
      List<Run> runs,
      List<Lake> countryLakes,
      double tolerance,
      String longestRiver,
      Path outDir)
      throws IOException {
    List<Kept> kept = CountryRiverLimits.simplified(runs, tolerance);
    if (kept.isEmpty()) {
      return Optional.empty();
    }
    int before = kept.size();
    kept = CountryRiverLimits.withinPointLimit(kept);
    int droppedForSize = before - kept.size();

    List<Lake> lakes =
        countryLakes.stream()
            .map(
                lake ->
                    new Lake(lake.areaKm2(), CountryLakes.simplifyLake(lake.polygons(), tolerance)))
            .toList();
    List<Lake> keptLakes = CountryLakes.withinLakeLimit(lakes);

    List<Kept> major =
        kept.stream().filter(k -> k.run().scalerank() <= MAJOR_MAX_SCALERANK).toList();
    List<Kept> minor =
        kept.stream().filter(k -> k.run().scalerank() > MAJOR_MAX_SCALERANK).toList();

    CountryRivers rivers =
        new CountryRivers(
            name,
            lineData(name, major),
            lineData(name, minor),
            CountryLakes.lakeData(name, keptLakes),
            longestRiver);
    String file = convertCountryNameToFileName(name);
    Path path = outDir.resolve(file);
    Files.writeString(path, GeoJson.prettyWriter().writeValueAsString(rivers) + "\n");
    CountryRiversIndexEntry entry =
        new CountryRiversIndexEntry(
            name,
            file,
            (int) kept.stream().map(k -> k.run().rivernum()).distinct().count(),
            Math.round(CountryRiverLimits.km(kept)),
            keptLakes.size());
    return Optional.of(
        new Written(
            rivers,
            entry,
            CountryRiverLimits.km(major),
            CountryRiverLimits.km(minor),
            droppedForSize,
            lakes.size() - keptLakes.size(),
            Files.size(path)));
  }

  private static GeoData lineData(String name, List<Kept> kept) {
    List<List<List<Double>>> lines = new ArrayList<>();
    for (Kept k : kept) {
      lines.add(GeoPoints.toPositions(k.run().points()));
    }
    return GeoData.feature(name, new GeoGeometry("MultiLineString", lines));
  }
}
