package com.parkinson.benjamin.geogames.loaders.lakes;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.CapturedOutput;
import com.parkinson.benjamin.geogames.model.Lake;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LakeLoaderMainTest {

  @TempDir Path out;

  private Path fixtures;
  private Path countries;

  @BeforeEach
  void locateFixtures() throws URISyntaxException {
    fixtures = Path.of(getClass().getResource("/fixtures/loaders/lakes").toURI());
    countries = Path.of(getClass().getResource("/fixtures/loaders/countries-processed").toURI());
  }

  private LakeLoaderMain.Report run(Path outDir) throws IOException {
    return LakeLoaderMain.run(
        fixtures.resolve("naturalearth.geojson"),
        fixtures.resolve("wikidata.json"),
        fixtures.resolve("overrides.json"),
        countries,
        outDir);
  }

  private Lake read(String fileName) throws IOException {
    return GeoJson.lenientReader().readValue(out.resolve(fileName).toFile(), Lake.class);
  }

  private List<String> fileNames(Path dir) throws IOException {
    try (Stream<Path> files = Files.list(dir)) {
      return files.map(p -> p.getFileName().toString()).sorted().toList();
    }
  }

  @Test
  void writesTheSelectedLakesSortedByName() throws IOException {
    LakeLoaderMain.Report report = run(out);

    assertThat(report.lakes())
        .extracting(LakeCandidate::name)
        .containsExactly("Big Lake", "Border Lake", "Diamond Lake", "Override Land", "Pond");
    assertThat(fileNames(out))
        .containsExactly(
            "big-lake-Q201.json",
            "border-lake-Q202.json",
            "diamond-lake-Q203.json",
            "override-land-Q206.json",
            "pond-Q205.json");
  }

  @Test
  void aLakeUnder100SquareKilometresIsDroppedUnlessAnOverrideNamesIt() throws IOException {
    LakeLoaderMain.Report report = run(out);

    assertThat(fileNames(out)).contains("pond-Q205.json").doesNotContain("puddle-Q204.json");
    assertThat(report.assembly().dropped()).containsEntry("smaller than 100 km²", 1);
    assertThat(read("pond-Q205.json").additionalNames()).contains("The Pond");
  }

  @Test
  void excludedAndUnknownLakesAreNotWritten() throws IOException {
    LakeLoaderMain.Report report = run(out);

    assertThat(report.assembly().dropped())
        .containsEntry("excluded by override", 1)
        .containsEntry("no Wikidata entry", 1);
    assertThat(fileNames(out)).noneMatch(name -> name.contains("Q208") || name.contains("Q209"));
  }

  @Test
  void aCountriesOverrideReplacesTheLocatedCountries() throws IOException {
    run(out);

    // The lake lies wholly in Beta, but the override says otherwise.
    assertThat(read("override-land-Q206.json").countries()).containsExactly("Zedland");
  }

  @Test
  void countriesAreTheOnesWithAtLeastFiveKilometresOfShore() throws IOException {
    run(out);

    assertThat(read("big-lake-Q201.json").countries()).containsExactly("Alpha");
    assertThat(read("border-lake-Q202.json").countries()).containsExactly("Alpha", "Beta");
    // The tip of this lake pokes about 2 km into Beta, which is under the minimum.
    assertThat(read("diamond-lake-Q203.json").countries()).containsExactly("Alpha");
  }

  @Test
  void aFamousLakeWithNoContinentIsReportedAsUnresolvedAndNotWritten() throws IOException {
    LakeLoaderMain.Report report = run(out);

    assertThat(report.selection().unresolved())
        .extracting(LakeCandidate::name)
        .containsExactly("No Continent");
    assertThat(fileNames(out)).noneMatch(name -> name.contains("Q207"));
  }

  @Test
  void eachFileHoldsOnePolygonLake() throws IOException {
    run(out);

    Lake lake = read("big-lake-Q201.json");

    assertThat(lake.geoData().geometry().type()).isEqualTo("Polygon");
    assertThat(lake.continent()).isEqualTo("Europe");
  }

  @Test
  void clearsOldJsonFilesButLeavesOtherFilesInTheOutputDirectory() throws IOException {
    Files.writeString(out.resolve("stale-Q1.json"), "{}");
    Files.writeString(out.resolve("README.txt"), "keep");

    run(out);

    assertThat(fileNames(out)).doesNotContain("stale-Q1.json").contains("README.txt");
  }

  @Test
  void runningTwiceGivesIdenticalFiles() throws IOException {
    Path first = out.resolve("first");
    Path second = out.resolve("second");

    run(first);
    run(second);

    assertThat(fileNames(first)).isEqualTo(fileNames(second));
    for (String name : fileNames(first)) {
      assertThat(Files.readAllBytes(first.resolve(name)))
          .isEqualTo(Files.readAllBytes(second.resolve(name)));
    }
  }

  @Test
  void mainPrintsTheSummary() throws Exception {
    String printed =
        CapturedOutput.of(
            () ->
                LakeLoaderMain.main(
                    new String[] {
                      fixtures.resolve("naturalearth.geojson").toString(),
                      fixtures.resolve("wikidata.json").toString(),
                      fixtures.resolve("overrides.json").toString(),
                      countries.toString(),
                      out.toString()
                    }));

    assertThat(printed)
        .startsWith("Written: 5\n  Asia: 1\n  Europe: 4\nDropped:\n")
        .contains("  1 excluded by override\n")
        .contains("  1 no Wikidata entry\n")
        .contains("  0 beyond the continent quotas\n")
        .contains("  1 unresolved continent (below)\n")
        .contains(
            "Skipped, unresolved continent (add overrides):\n  No Continent Q207 sitelinks=1000")
        .contains("Lakes with no countries (add overrides):\nLakes:\n")
        .contains("  Big Lake | Q201 | Europe | sitelinks=50 |")
        .contains("| 5 points | [Alpha] | [Big]\n")
        .contains("| [Zedland] | [Lake Override Land] [override]\n")
        .contains("| [Alpha] | [Lake Pond, The Pond] [override]\n");
    assertThat(fileNames(out)).hasSize(5);
  }
}
