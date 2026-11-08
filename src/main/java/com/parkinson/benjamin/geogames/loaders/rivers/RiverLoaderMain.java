package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.countries.ProcessedCountryReader;
import com.parkinson.benjamin.geogames.loaders.features.CountryLocator;
import com.parkinson.benjamin.geogames.loaders.features.FeatureSelector;
import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthFeature;
import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthReader;
import com.parkinson.benjamin.geogames.loaders.features.WikidataEntry;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import tools.jackson.core.type.TypeReference;

/**
 * Usage: RiverLoaderMain &lt;naturalEarth.geojson&gt; &lt;wikidata.json&gt; &lt;overrides.json&gt;
 * &lt;osmDir&gt; &lt;countriesDir&gt; &lt;outDir&gt;
 *
 * <p>Picks the most famous rivers of each continent, up to {@link #RIVER_QUOTAS}.
 *
 * <p>Also works out each river's source, mouth and countries (see {@link RiverEnds} and {@link
 * CountryLocator}). Offline and deterministic: reads no network. Clears the {@code *.json} files in outDir before
 * writing.
 */
public class RiverLoaderMain {

  /** How many rivers each continent gets, so the game isn't all Europe and Asia. */
  public static final Map<String, Integer> RIVER_QUOTAS =
      Map.of(
          "Europe", 13,
          "Asia", 11,
          "Africa", 9,
          "North America", 6,
          "South America", 8,
          "Australia", 3);

  private static final double LOW_CONFIDENCE = 0.6;

  /**
   * What a run produced, for the summary.
   *
   * @param rivers the rivers written, sorted by name then QID
   * @param files the files written, in the same order
   * @param assembly the candidates and why the others were dropped
   * @param selection what the continent quotas kept and skipped
   * @param overrides the overrides that were read, by QID
   * @param facts source, mouth and countries of each written river, by QID
   * @param unlocatedKm the km of each written river that is in no country, by QID
   */
  public record Report(
      List<RiverCandidate> rivers,
      List<Path> files,
      RiverAssembler.Assembly assembly,
      FeatureSelector.Selection<RiverCandidate> selection,
      Map<String, RiverOverride> overrides,
      Map<String, RiverFacts> facts,
      Map<String, Double> unlocatedKm) {}

  public static void main(String[] args) throws IOException {
    Report report =
        run(
            Path.of(args[0]),
            Path.of(args[1]),
            Path.of(args[2]),
            Path.of(args[3]),
            Path.of(args[4]),
            Path.of(args[5]));
    printSummary(report.rivers(), report.files(), report.assembly(), report.selection());
    printFactsSummary(report.rivers(), report.overrides(), report.facts(), report.unlocatedKm());
  }

  /** Does everything {@code main} does except print. */
  static Report run(
      Path naturalEarth,
      Path wikidataFile,
      Path overridesFile,
      Path osmDir,
      Path countriesDir,
      Path outDir)
      throws IOException {
    Map<String, NaturalEarthFeature> groups =
        new NaturalEarthReader().read(naturalEarth, Set.of("Canal"));
    Map<String, WikidataEntry> wikidata = readJson(wikidataFile, new TypeReference<>() {});
    Map<String, RiverOverride> overrides = readJson(overridesFile, new TypeReference<>() {});

    RiverAssembler.Assembly assembly =
        new RiverAssembler(file -> readOsmLines(osmDir.resolve(file)))
            .assemble(groups, wikidata, overrides);
    FeatureSelector.Selection<RiverCandidate> selection =
        FeatureSelector.select(assembly.candidates(), RIVER_QUOTAS);

    List<RiverCandidate> rivers = new ArrayList<>(selection.selected());
    rivers.sort(Comparator.comparing(RiverCandidate::name).thenComparing(RiverCandidate::qid));

    CountryLocator locator =
        new CountryLocator(new ProcessedCountryReader().readCountries(countriesDir), 10);
    Map<String, RiverFacts> facts = new LinkedHashMap<>();
    Map<String, Double> unlocatedKm = new LinkedHashMap<>();
    for (RiverCandidate river : rivers) {
      CountryLocator.Result located = locator.locate(river.lines());
      facts.put(
          river.qid(), RiverFacts.compute(river.lines(), overrides.get(river.qid()), located));
      unlocatedKm.put(river.qid(), located.unlocatedKm());
    }
    List<Path> files = new RiverWriter().write(rivers, facts, outDir);

    return new Report(rivers, files, assembly, selection, overrides, facts, unlocatedKm);
  }

  private static void printFactsSummary(
      List<RiverCandidate> rivers,
      Map<String, RiverOverride> overrides,
      Map<String, RiverFacts> facts,
      Map<String, Double> unlocatedKm) {
    System.out.println("Low flow confidence (< " + LOW_CONFIDENCE + "), no override decision:");
    for (RiverCandidate c : rivers) {
      RiverFacts f = facts.get(c.qid());
      RiverOverride override = overrides.get(c.qid());
      boolean decided =
          override != null
              && (override.reverseFlow() != null
                  || (override.source() != null && override.mouth() != null));
      if (f.flowConfidence() < LOW_CONFIDENCE && !decided) {
        System.out.printf(
            "  %s %s confidence=%.2f source=%s mouth=%s%n",
            c.name(), c.qid(), f.flowConfidence(), f.source(), f.mouth());
      }
    }

    Map<Integer, Integer> perCount = new TreeMap<>();
    rivers.forEach(c -> perCount.merge(facts.get(c.qid()).countries().size(), 1, Integer::sum));
    System.out.println("Rivers by number of countries:");
    perCount.forEach((count, n) -> System.out.println("  " + count + " countries: " + n));

    System.out.println("Rivers with more than 50 km in no country:");
    for (RiverCandidate c : rivers) {
      if (unlocatedKm.get(c.qid()) > 50) {
        System.out.printf("  %s %s %.0f km%n", c.name(), c.qid(), unlocatedKm.get(c.qid()));
      }
    }
  }

  private static void printSummary(
      List<RiverCandidate> rivers,
      List<Path> files,
      RiverAssembler.Assembly assembly,
      FeatureSelector.Selection<RiverCandidate> selection)
      throws IOException {
    System.out.println("Written: " + rivers.size());
    Map<String, Integer> perContinent = new TreeMap<>();
    rivers.forEach(c -> perContinent.merge(c.continent(), 1, Integer::sum));
    perContinent.forEach((continent, n) -> System.out.println("  " + continent + ": " + n));
    System.out.println("Dropped:");
    assembly.dropped().forEach((reason, n) -> System.out.println("  " + n + " " + reason));
    System.out.println("  " + selection.beyondLimit() + " beyond the continent quotas");
    System.out.println("  " + selection.unresolved().size() + " unresolved continent (below)");

    long total = 0;
    long largest = 0;
    Path largestFile = null;
    for (Path file : files) {
      long size = Files.size(file);
      total += size;
      if (size > largest) {
        largest = size;
        largestFile = file;
      }
    }
    System.out.println(
        "Total bytes: " + total + ", largest: " + largest + " (" + largestFile + ")");

    System.out.println("Skipped, unresolved continent (add overrides):");
    for (RiverCandidate c : selection.unresolved()) {
      System.out.println("  " + c.name() + " " + c.qid() + " sitelinks=" + c.sitelinks());
    }

    System.out.println("Rivers:");
    for (RiverCandidate c : rivers) {
      System.out.printf(
          "  %s | %s | %s | sitelinks=%d | %.0f km | %d points | %s%s%n",
          c.name(),
          c.qid(),
          c.continent(),
          c.sitelinks(),
          c.km(),
          RiverGeometry.pointCount(c.lines()),
          c.aliases(),
          c.fromOverride() ? " [override]" : "");
    }
  }

  private static List<List<double[]>> readOsmLines(Path file) {
    try (InputStream in = Files.newInputStream(file)) {
      GeoData data = GeoJson.lenientReader().readValue(in, GeoData.class);
      List<List<double[]>> lines = new ArrayList<>();
      for (List<List<Number>> line : (List<List<List<Number>>>) data.geometry().coordinates()) {
        List<double[]> points = new ArrayList<>();
        for (List<Number> p : line) {
          points.add(new double[] {p.get(0).doubleValue(), p.get(1).doubleValue()});
        }
        lines.add(points);
      }
      return lines;
    } catch (IOException e) {
      throw new java.io.UncheckedIOException("Couldn't read " + file, e);
    }
  }

  private static <T> T readJson(Path file, TypeReference<T> type) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return GeoJson.lenientReader().readValue(in, type);
    }
  }
}
