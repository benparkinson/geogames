package com.parkinson.benjamin.geogames.loaders.countries;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.CapturedOutput;
import com.parkinson.benjamin.geogames.model.Bounds;
import com.parkinson.benjamin.geogames.model.CountryShape;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CountryShapeMainTest {

  @TempDir Path out;

  private Path countries;

  @BeforeEach
  void locateFixtures() throws URISyntaxException {
    countries = Path.of(getClass().getResource("/fixtures/loaders/country-shapes").toURI());
  }

  private static CountryShape find(CountryShapeMain.Report report, String name) {
    return report.shapes().stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
  }

  private static Set<String> points(Object coordinates) {
    Set<String> points = new HashSet<>();
    collect(coordinates, points);
    return points;
  }

  private static void collect(Object o, Set<String> points) {
    List<?> list = (List<?>) o;
    if (!list.isEmpty() && list.get(0) instanceof Number) {
      points.add(list.get(0) + "," + list.get(1));
      return;
    }
    for (Object child : list) {
      collect(child, points);
    }
  }

  private List<String> fileNames(Path dir) throws IOException {
    try (Stream<Path> files = Files.list(dir)) {
      return files.map(p -> p.getFileName().toString()).sorted().toList();
    }
  }

  @Test
  void writesOneFilePerCountryAndCountsTheBytes() throws IOException {
    CountryShapeMain.Report report = CountryShapeMain.run(countries, out);

    assertThat(fileNames(out)).containsExactly("Alpha.json", "Beta.json", "GammaIsland.json");
    long size = 0;
    for (String name : fileNames(out)) {
      size += Files.size(out.resolve(name));
    }
    assertThat(report.bytes()).isEqualTo(size);
    assertThat(report.shapes()).hasSize(3);
  }

  @Test
  void neighboursAreTheCountriesSharingAnEdge() throws IOException {
    CountryShapeMain.Report report = CountryShapeMain.run(countries, out);

    assertThat(find(report, "Alpha").neighbours())
        .singleElement()
        .satisfies(
            n -> {
              assertThat(n.name()).isEqualTo("Beta");
              assertThat(n.mainland()).isTrue();
            });
    assertThat(find(report, "Beta").neighbours())
        .singleElement()
        .satisfies(n -> assertThat(n.name()).isEqualTo("Alpha"));
    assertThat(find(report, "Gamma Island").neighbours()).isEmpty();
  }

  @Test
  void aCountryWithNoNeighbourHasNoLandBorderAndAllCoast() throws IOException {
    CountryShapeMain.Report report = CountryShapeMain.run(countries, out);

    CountryShape island = find(report, "Gamma Island");
    assertThat(island.landBorderKm()).isZero();
    assertThat(island.coastKm()).isGreaterThan(700); // a 2 x 2 degree square
    assertThat(island.landBorders().geometry().coordinates()).isEmpty();
  }

  @Test
  void theSharedEdgeIsALandBorderAndTheRestIsCoast() throws IOException {
    CountryShapeMain.Report report = CountryShapeMain.run(countries, out);

    CountryShape alpha = find(report, "Alpha");
    assertThat(alpha.landBorderKm()).isBetween(1100.0, 1120.0); // 10 degrees of longitude line
    assertThat(alpha.coastKm()).isBetween(3300.0, 3350.0); // the other three sides
    assertThat(points(alpha.coast().geometry().coordinates())).doesNotContain("10.0,5.0");
  }

  @Test
  void theSharedBorderIsIdenticalOnBothSides() throws IOException {
    CountryShapeMain.Report report = CountryShapeMain.run(countries, out);

    Set<String> alphaSide = points(find(report, "Alpha").landBorders().geometry().coordinates());
    Set<String> betaSide = points(find(report, "Beta").landBorders().geometry().coordinates());

    assertThat(alphaSide).isNotEmpty().isEqualTo(betaSide);
  }

  @Test
  void focusIsTheBoundingBoxOfTheCountry() throws IOException {
    CountryShapeMain.Report report = CountryShapeMain.run(countries, out);

    assertThat(find(report, "Alpha").focus()).isEqualTo(new Bounds(0, 0, 10, 10));
    assertThat(find(report, "Gamma Island").focus()).isEqualTo(new Bounds(30, 0, 32, 2));
  }

  @Test
  void eachFileReadsBackAsTheShapeInTheReport() throws IOException {
    CountryShapeMain.Report report = CountryShapeMain.run(countries, out);

    CountryShape read =
        GeoJson.lenientReader().readValue(out.resolve("Beta.json").toFile(), CountryShape.class);

    assertThat(read).isEqualTo(find(report, "Beta"));
    assertThat(Files.readString(out.resolve("Beta.json"))).endsWith("}\n");
  }

  @Test
  void createsTheOutputDirectoryAndClearsOldJsonFiles() throws IOException {
    Path nested = out.resolve("nested");
    Files.createDirectories(nested);
    Files.writeString(nested.resolve("Old.json"), "{}");
    Files.writeString(nested.resolve("notes.txt"), "keep");

    CountryShapeMain.run(countries, nested);
    CountryShapeMain.run(countries, out.resolve("fresh"));

    assertThat(fileNames(nested)).doesNotContain("Old.json").contains("notes.txt");
    assertThat(fileNames(out.resolve("fresh"))).hasSize(3);
  }

  @Test
  void runningTwiceGivesIdenticalFiles() throws IOException {
    Path first = out.resolve("first");
    Path second = out.resolve("second");

    CountryShapeMain.run(countries, first);
    CountryShapeMain.run(countries, second);

    for (String name : fileNames(first)) {
      assertThat(Files.readAllBytes(first.resolve(name)))
          .isEqualTo(Files.readAllBytes(second.resolve(name)));
    }
  }

  @Test
  void mainPrintsTheSummary() throws Exception {
    String printed =
        CapturedOutput.of(
            () -> CountryShapeMain.main(new String[] {countries.toString(), out.toString()}));

    assertThat(printed)
        .startsWith("Written: 3\nNo coast: 0\nNo land border: 1\n")
        .contains("2+ mainland neighbours: 0\n")
        .contains("Eligible, neighbours game: 0\n")
        .contains("Eligible, coastline game: 2\n")
        .contains("Eligible, land borders game: 2\n")
        .contains("Largest by points:\n")
        .contains("  Alpha: ")
        .contains("  Gamma Island: ")
        .contains("Total bytes: ");
  }
}
