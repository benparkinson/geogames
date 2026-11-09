package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.CapturedOutput;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.River;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RiverLoaderMainTest {

  @TempDir Path out;

  private Path fixtures;
  private Path countries;

  @BeforeEach
  void locateFixtures() throws URISyntaxException {
    fixtures = Path.of(getClass().getResource("/fixtures/loaders/rivers").toURI());
    countries = Path.of(getClass().getResource("/fixtures/loaders/countries-processed").toURI());
  }

  private RiverLoaderMain.Report run(Path outDir) throws IOException {
    return RiverLoaderMain.run(
        fixtures.resolve("naturalearth.geojson"),
        fixtures.resolve("wikidata.json"),
        fixtures.resolve("overrides.json"),
        countries,
        outDir);
  }

  private River read(String fileName) throws IOException {
    return GeoJson.lenientReader().readValue(out.resolve(fileName).toFile(), River.class);
  }

  private List<String> fileNames(Path dir) throws IOException {
    try (Stream<Path> files = Files.list(dir)) {
      return files.map(p -> p.getFileName().toString()).sorted().toList();
    }
  }

  @Test
  void writesTheSelectedRiversSortedByName() throws IOException {
    RiverLoaderMain.Report report = run(out);

    assertThat(report.rivers())
        .extracting(RiverCandidate::name)
        .containsExactly("Alpha River", "Backwards", "Cross River", "New Name", "Piecewise River");
    assertThat(fileNames(out))
        .containsExactly(
            "alpha-river-Q101.json",
            "backwards-Q105.json",
            "cross-river-Q102.json",
            "new-name-Q104.json",
            "piecewise-river-Q107.json");
    assertThat(report.files()).hasSize(5).allSatisfy(file -> assertThat(file).exists());
  }

  @Test
  void anExcludedRiverIsNotWritten() throws IOException {
    RiverLoaderMain.Report report = run(out);

    assertThat(fileNames(out)).noneMatch(name -> name.contains("excluded"));
    assertThat(report.assembly().dropped()).containsEntry("excluded by override", 1);
  }

  @Test
  void riversWithNoDataOrTooShortAreCountedAsDropped() throws IOException {
    RiverLoaderMain.Report report = run(out);

    assertThat(report.assembly().dropped())
        .containsEntry("no Wikidata entry", 1)
        .containsEntry("shorter than 200 km", 1);
  }

  @Test
  void aRenamedRiverIsWrittenUnderItsNewName() throws IOException {
    run(out);

    River river = read("new-name-Q104.json");

    assertThat(river.name()).isEqualTo("New Name");
    assertThat(river.id()).isEqualTo("Q104");
    assertThat(river.continent()).isEqualTo("Asia");
  }

  @Test
  void reverseFlowSwapsTheComputedSourceAndMouth() throws IOException {
    run(out);

    River plain = read("alpha-river-Q101.json");
    River reversed = read("backwards-Q105.json");

    // Both lines are digitised west to east, so the computed source is the west end.
    assertThat(plain.source()).isEqualTo(new Coordinate(5, 1));
    assertThat(plain.mouth()).isEqualTo(new Coordinate(5, 9));
    assertThat(reversed.source()).isEqualTo(new Coordinate(9, 9));
    assertThat(reversed.mouth()).isEqualTo(new Coordinate(9, 1));
  }

  @Test
  void aFamousRiverWithNoContinentIsReportedAsUnresolvedAndNotWritten() throws IOException {
    RiverLoaderMain.Report report = run(out);

    assertThat(report.selection().unresolved())
        .extracting(RiverCandidate::name)
        .containsExactly("No Continent");
    assertThat(fileNames(out)).noneMatch(name -> name.contains("no-continent"));
  }

  @Test
  void eachRiverListsTheCountriesItFlowsThroughLongestFirst() throws IOException {
    run(out);

    assertThat(read("alpha-river-Q101.json").countries()).containsExactly("Alpha");
    assertThat(read("cross-river-Q102.json").countries()).containsExactly("Alpha", "Beta");
    assertThat(read("new-name-Q104.json").countries()).containsExactly("Beta");
  }

  @Test
  void mainPieceOnlyDropsTheDetachedLine() throws IOException {
    run(out);

    River river = read("piecewise-river-Q107.json");

    assertThat(river.geoData().geometry().coordinates())
        .isEqualTo(List.of(List.of(List.of(11.0, 5.0), List.of(19.0, 5.0))));
    assertThat(river.countries()).containsExactly("Beta");
  }

  @Test
  void aNameVariantBecomesAnAlias() throws IOException {
    run(out);

    assertThat(read("alpha-river-Q101.json").additionalNames()).contains("Alphie", "Alpha");
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
                RiverLoaderMain.main(
                    new String[] {
                      fixtures.resolve("naturalearth.geojson").toString(),
                      fixtures.resolve("wikidata.json").toString(),
                      fixtures.resolve("overrides.json").toString(),
                      countries.toString(),
                      out.toString()
                    }));

    assertThat(printed)
        .startsWith("Written: 5\n  Asia: 2\n  Europe: 3\nDropped:\n")
        .contains("  1 excluded by override\n")
        .contains("  1 no Wikidata entry\n")
        .contains("  1 shorter than 200 km\n")
        .contains("  0 beyond the continent quotas\n")
        .contains("  1 unresolved continent (below)\n")
        .contains(
            "Skipped, unresolved continent (add overrides):\n"
                + "  No Continent Q106 sitelinks=1000\n")
        .contains(
            "  Alpha River | Q101 | Europe | sitelinks=50 | 886 km | 2 points | [Alphie, Alpha]\n")
        .contains("  New Name | Q104 | Asia | sitelinks=30 |")
        .contains("Rivers by number of countries:\n  1 countries: 4\n  2 countries: 1\n")
        .contains("Rivers with more than 50 km in no country:\n");
    assertThat(fileNames(out)).hasSize(5);
  }

  @Test
  void mainListsRiversWhoseFlowDirectionIsUncertainAndUndecided() throws Exception {
    // Two lines digitised in opposite directions: the vote is split.
    Path dir = out.resolve("split");
    Files.createDirectories(dir);
    Files.writeString(
        dir.resolve("ne.geojson"),
        """
        {"type": "FeatureCollection", "features": [
          {"type": "Feature",
           "properties": {"featurecla": "River", "name": "Split", "wikidataid": "Q1"},
           "geometry": {"type": "MultiLineString",
                        "coordinates": [[[1, 5], [3, 5]], [[5, 5], [3, 5]]]}}
        ]}
        """);
    Files.writeString(
        dir.resolve("wikidata.json"),
        """
        {"Q1": {"label": "Split", "sitelinks": 5, "continents": ["Europe"]}}
        """);
    Files.writeString(dir.resolve("overrides.json"), "{}");

    String printed =
        CapturedOutput.of(
            () ->
                RiverLoaderMain.main(
                    new String[] {
                      dir.resolve("ne.geojson").toString(),
                      dir.resolve("wikidata.json").toString(),
                      dir.resolve("overrides.json").toString(),
                      countries.toString(),
                      dir.resolve("out").toString()
                    }));

    assertThat(printed)
        .contains("Low flow confidence (< 0.6), no override decision:\n  Split Q1 confidence=");
  }
}
