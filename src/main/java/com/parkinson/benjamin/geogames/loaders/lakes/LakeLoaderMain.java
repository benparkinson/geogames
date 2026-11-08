package com.parkinson.benjamin.geogames.loaders.lakes;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.countries.ProcessedCountryReader;
import com.parkinson.benjamin.geogames.loaders.features.CountryLocator;
import com.parkinson.benjamin.geogames.loaders.features.FeatureSelector;
import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthFeature;
import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthReader;
import com.parkinson.benjamin.geogames.loaders.features.WikidataEntry;
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
 * Usage: LakeLoaderMain &lt;naturalEarth.geojson&gt; &lt;wikidata.json&gt; &lt;overrides.json&gt;
 * &lt;countriesDir&gt; &lt;outDir&gt;
 *
 * <p>Picks the most famous lakes of each continent, up to {@link #LAKE_QUOTAS}, locates the
 * countries on each shore and writes one {@code Lake} per file, named slug-QID.json. Offline and
 * deterministic. Clears the {@code *.json} files in outDir before writing.
 */
public class LakeLoaderMain {

  /** How many lakes each continent gets. */
  public static final Map<String, Integer> LAKE_QUOTAS =
      Map.of(
          "Europe", 12,
          "Asia", 10,
          "Africa", 8,
          "North America", 10,
          "South America", 5,
          "Australia", 3);

  /** Lakes are smaller than rivers, so a country needs less shore to count. */
  static final double MIN_SHORE_KM = 5;

  public static void main(String[] args) throws IOException {
    Path countriesDir = Path.of(args[3]);
    Path outDir = Path.of(args[4]);

    Map<String, NaturalEarthFeature> groups =
        new NaturalEarthReader().read(Path.of(args[0]), Set.of());
    Map<String, WikidataEntry> wikidata = readJson(Path.of(args[1]), new TypeReference<>() {});
    Map<String, LakeOverride> overrides = readJson(Path.of(args[2]), new TypeReference<>() {});

    LakeAssembler.Assembly assembly = new LakeAssembler().assemble(groups, wikidata, overrides);
    FeatureSelector.Selection<LakeCandidate> selection =
        FeatureSelector.select(assembly.candidates(), LAKE_QUOTAS);

    List<LakeCandidate> lakes = new ArrayList<>(selection.selected());
    lakes.sort(Comparator.comparing(LakeCandidate::name).thenComparing(LakeCandidate::qid));

    CountryLocator locator =
        new CountryLocator(new ProcessedCountryReader().readCountries(countriesDir), MIN_SHORE_KM);
    Map<String, List<String>> countries = new LinkedHashMap<>();
    for (LakeCandidate lake : lakes) {
      LakeOverride override = overrides.get(lake.qid());
      countries.put(
          lake.qid(),
          override != null && override.countries() != null
              ? override.countries()
              : locator.locateRings(lake.polygons()).countries());
    }
    List<Path> files = new LakeWriter().write(lakes, countries, outDir);

    printSummary(lakes, countries, files, assembly, selection);
  }

  private static void printSummary(
      List<LakeCandidate> lakes,
      Map<String, List<String>> countries,
      List<Path> files,
      LakeAssembler.Assembly assembly,
      FeatureSelector.Selection<LakeCandidate> selection)
      throws IOException {
    System.out.println("Written: " + lakes.size());
    Map<String, Integer> perContinent = new TreeMap<>();
    lakes.forEach(c -> perContinent.merge(c.continent(), 1, Integer::sum));
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
    for (LakeCandidate c : selection.unresolved()) {
      System.out.printf(
          "  %s %s sitelinks=%d %.0f km²%n", c.name(), c.qid(), c.sitelinks(), c.areaKm2());
    }

    System.out.println("Lakes with no countries (add overrides):");
    for (LakeCandidate c : lakes) {
      if (countries.get(c.qid()).isEmpty()) {
        System.out.println("  " + c.name() + " " + c.qid());
      }
    }

    System.out.println("Lakes:");
    for (LakeCandidate c : lakes) {
      System.out.printf(
          "  %s | %s | %s | sitelinks=%d | %.0f km² | %d points | %s | %s%s%n",
          c.name(),
          c.qid(),
          c.continent(),
          c.sitelinks(),
          c.areaKm2(),
          LakeGeometry.pointCount(c.polygons()),
          countries.get(c.qid()),
          c.aliases(),
          c.fromOverride() ? " [override]" : "");
    }
  }

  private static <T> T readJson(Path file, TypeReference<T> type) throws IOException {
    try (InputStream in = Files.newInputStream(file)) {
      return GeoJson.lenientReader().readValue(in, type);
    }
  }
}
