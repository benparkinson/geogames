package com.parkinson.benjamin.geogames.loaders.countries;

import static com.parkinson.benjamin.geogames.helper.CountryHelper.convertCountryNameToFileName;

import com.parkinson.benjamin.geogames.helper.CountryShapeEligibility;
import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.model.CountryShape;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Usage: CountryShapeMain &lt;processedCountriesDir&gt; &lt;outDir&gt;
 *
 * <p>Builds the simplified display shapes with their coast, land borders and neighbours, and writes
 * one file per country. Clears the {@code *.json} files in outDir first.
 */
public class CountryShapeMain {

  /**
   * What a run produced, for the summary.
   *
   * @param shapes the shapes written
   * @param bytes the total size of the files written
   */
  public record Report(List<CountryShape> shapes, long bytes) {}

  public static void main(String[] args) throws IOException {
    Report report = run(Path.of(args[0]), Path.of(args[1]));
    printSummary(report.shapes(), report.bytes());
  }

  /** Does everything {@code main} does except print. */
  static Report run(Path processedCountriesDir, Path outDir) throws IOException {
    List<CountryShape> shapes =
        CountryShapeBuilder.build(
            new ProcessedCountryReader().readCountries(processedCountriesDir));

    Files.createDirectories(outDir);
    new CountryWriter().clearOutput(outDir);
    long bytes = 0;
    for (CountryShape shape : shapes) {
      String json = GeoJson.prettyWriter().writeValueAsString(shape) + "\n";
      Path file = outDir.resolve(convertCountryNameToFileName(shape.name()));
      Files.writeString(file, json);
      bytes += Files.size(file);
    }
    return new Report(shapes, bytes);
  }

  private static void printSummary(List<CountryShape> shapes, long bytes) {
    System.out.println("Written: " + shapes.size());
    System.out.println("No coast: " + count(shapes, s -> s.coastKm() == 0));
    System.out.println("No land border: " + count(shapes, s -> s.landBorderKm() == 0));
    System.out.println(
        "2+ mainland neighbours: " + count(shapes, CountryShapeEligibility.neighbours()));
    System.out.println(
        "Eligible, neighbours game: " + count(shapes, CountryShapeEligibility.neighbours()));
    System.out.println(
        "Eligible, coastline game: " + count(shapes, CountryShapeEligibility.coastline()));
    System.out.println(
        "Eligible, land borders game: " + count(shapes, CountryShapeEligibility.landBorders()));
    System.out.println("Largest by points:");
    shapes.stream()
        .sorted(Comparator.comparingInt(CountryShapeMain::points).reversed())
        .limit(5)
        .forEach(s -> System.out.println("  " + s.name() + ": " + points(s)));
    System.out.println("Total bytes: " + bytes);
  }

  private static long count(List<CountryShape> shapes, Predicate<CountryShape> test) {
    return shapes.stream().filter(test).count();
  }

  static int points(CountryShape shape) {
    return CountryShapeBuilder.pointCount(shape.outline())
        + CountryShapeBuilder.pointCount(shape.coast())
        + CountryShapeBuilder.pointCount(shape.landBorders());
  }
}
