package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;

/**
 * Usage: RiverLoaderMain &lt;naturalEarth.geojson&gt; &lt;wikidata.json&gt; &lt;overrides.json&gt;
 * &lt;osmDir&gt; &lt;outDir&gt; [maxRivers=150]
 *
 * <p>Offline and deterministic: reads no network. Clears the {@code *.json} files in outDir before
 * writing.
 */
public class RiverLoaderMain {

  public static void main(String[] args) throws IOException {
    Path osmDir = Path.of(args[3]);
    Path outDir = Path.of(args[4]);
    int maxRivers = args.length > 5 ? Integer.parseInt(args[5]) : 150;

    Map<String, NaturalEarthRiver> groups = new NaturalEarthReader().read(Path.of(args[0]));
    Map<String, WikidataEntry> wikidata = readJson(Path.of(args[1]), new TypeReference<>() {});
    Map<String, RiverOverride> overrides = readJson(Path.of(args[2]), new TypeReference<>() {});

    RiverAssembler.Assembly assembly =
        new RiverAssembler(file -> readOsmLines(osmDir.resolve(file)))
            .assemble(groups, wikidata, overrides);
    RiverSelector.Selection selection = RiverSelector.select(assembly.candidates(), maxRivers);

    List<RiverCandidate> rivers = new ArrayList<>(selection.selected());
    rivers.sort(Comparator.comparing(RiverCandidate::name).thenComparing(RiverCandidate::qid));
    List<Path> files = new RiverWriter().write(rivers, outDir);

    printSummary(rivers, files, assembly, selection);
  }

  private static void printSummary(
      List<RiverCandidate> rivers,
      List<Path> files,
      RiverAssembler.Assembly assembly,
      RiverSelector.Selection selection)
      throws IOException {
    System.out.println("Written: " + rivers.size());
    System.out.println("Dropped:");
    assembly.dropped().forEach((reason, n) -> System.out.println("  " + n + " " + reason));
    System.out.println("  " + selection.beyondLimit() + " beyond the limit");
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
