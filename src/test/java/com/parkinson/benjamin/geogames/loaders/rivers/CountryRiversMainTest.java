package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.CapturedOutput;
import com.parkinson.benjamin.geogames.loaders.rivers.CountryLakes.Lake;
import com.parkinson.benjamin.geogames.loaders.rivers.CountryRiverLimits.Kept;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverClipper.Run;
import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CountryRiversMainTest {

  @TempDir Path out;

  private Path rivers;
  private Path wikidata;
  private Path overrides;
  private Path lakes;
  private Path countries;

  @BeforeEach
  void locateFixtures() throws URISyntaxException {
    Path fixtures = Path.of(getClass().getResource("/fixtures/loaders/country-rivers").toURI());
    rivers = fixtures.resolve("rivers.geojson");
    wikidata = fixtures.resolve("wikidata.json");
    overrides = fixtures.resolve("overrides.json");
    lakes = fixtures.resolve("lakes.geojson");
    countries = Path.of(getClass().getResource("/fixtures/loaders/countries-processed").toURI());
  }

  private CountryRiversMain.Report run() throws IOException {
    return CountryRiversMain.run(rivers, wikidata, overrides, lakes, countries, out);
  }

  private static int lineCount(com.parkinson.benjamin.geogames.model.geojson.GeoData data) {
    return data.geometry().coordinates().size();
  }

  private static Run run(int scalerank, int rivernum, int points) {
    List<double[]> list = new ArrayList<>();
    for (int i = 0; i < points; i++) {
      list.add(new double[] {i, 0});
    }
    return new Run(scalerank, rivernum, null, list);
  }

  @Test
  void writesOneFileForEachCountryWithRiversAndAnIndex() throws IOException {
    CountryRiversMain.Report report = run();

    try (Stream<Path> files = Files.list(out)) {
      assertThat(files.map(p -> p.getFileName().toString()).sorted().toList())
          .containsExactly("Alpha.json", "Beta.json", "index.json");
    }
    assertThat(report.index())
        .containsExactly(
            new CountryRiversIndexEntry(
                "Alpha", "Alpha.json", 2, report.index().get(0).riverKm(), 1),
            new CountryRiversIndexEntry(
                "Beta", "Beta.json", 3, report.index().get(1).riverKm(), 2));
    assertThat(report.index().get(0).riverKm()).isBetween(1_700L, 1_800L);
  }

  @Test
  void splitsTheRiversIntoMajorAndMinorByScalerank() throws IOException {
    CountryRiversMain.Report report = run();

    CountryRivers alpha = report.rivers().get("Alpha");
    assertThat(lineCount(alpha.major())).isEqualTo(1); // Crossing, scalerank 3
    assertThat(lineCount(alpha.minor())).isEqualTo(1); // Along the border, scalerank 7
    CountryRivers beta = report.rivers().get("Beta");
    assertThat(lineCount(beta.major())).isEqualTo(1);
    assertThat(lineCount(beta.minor()))
        .isEqualTo(3); // the border river and two lake centerline lines
    assertThat(alpha.major().geometry().type()).isEqualTo("MultiLineString");
    assertThat(alpha.lakes().geometry().type()).isEqualTo("MultiPolygon");
  }

  @Test
  void aStubUnderFiveKilometresAndARiverInNoCountryAreDropped() throws IOException {
    CountryRiversMain.Report report = run();

    // 6 rivers in the file; the stub (3), the one in no country (5) and the point (6) are not kept.
    assertThat(report.index().stream().mapToInt(CountryRiversIndexEntry::riverCount).max())
        .hasValue(3);
    assertThat(report.kmByScalerank()).containsKeys(3, 5, 6, 7, 8);
  }

  @Test
  void aRiverAlongABorderGoesToBothCountries() throws IOException {
    CountryRiversMain.Report report = run();

    assertThat(lineCount(report.rivers().get("Alpha").minor())).isEqualTo(1);
    assertThat(lineCount(report.rivers().get("Beta").minor())).isGreaterThanOrEqualTo(1);
  }

  @Test
  void aLakeOnTheShoreOfTwoCountriesGoesWholeToBothAndASmallOneIsLeftOut() throws IOException {
    CountryRiversMain.Report report = run();

    CountryRivers alpha = report.rivers().get("Alpha");
    CountryRivers beta = report.rivers().get("Beta");
    // Shared goes to both; Island lake only to Beta; Puddle is under 100 km2.
    assertThat(alpha.lakes().geometry().coordinates()).hasSize(1);
    assertThat(beta.lakes().geometry().coordinates()).hasSize(2);
    assertThat(alpha.lakes().geometry().coordinates().get(0))
        .isEqualTo(beta.lakes().geometry().coordinates().get(0));
  }

  @Test
  void theFilesAreTheSameWhenRunTwiceAndAreReadBackAsTheModel() throws IOException {
    run();
    String first =
        Files.readString(out.resolve("Alpha.json")) + Files.readString(out.resolve("index.json"));
    run();
    String second =
        Files.readString(out.resolve("Alpha.json")) + Files.readString(out.resolve("index.json"));

    assertThat(second).isEqualTo(first);
    CountryRivers alpha =
        GeoJson.lenientReader().readValue(out.resolve("Alpha.json").toFile(), CountryRivers.class);
    assertThat(alpha.name()).isEqualTo("Alpha");
    assertThat(lineCount(alpha.major())).isEqualTo(1);
    CountryRiversIndexEntry[] index =
        GeoJson.lenientReader()
            .readValue(out.resolve("index.json").toFile(), CountryRiversIndexEntry[].class);
    assertThat(index).extracting(CountryRiversIndexEntry::name).containsExactly("Alpha", "Beta");
  }

  @Test
  void namesTheLongestNamedRiverOfEachCountryAndWritesItToTheFile() throws IOException {
    CountryRiversMain.Report report = run();

    assertThat(report.rivers().get("Alpha").longestRiver()).isEqualTo("Crossing");
    assertThat(report.rivers().get("Beta").longestRiver()).isEqualTo("Crossing");
    CountryRivers alpha =
        GeoJson.lenientReader().readValue(out.resolve("Alpha.json").toFile(), CountryRivers.class);
    assertThat(alpha.longestRiver()).isEqualTo("Crossing");
  }

  @Test
  void clearsOldFilesFromTheOutputFolder() throws IOException {
    Files.writeString(out.resolve("Old.json"), "{}");

    run();

    assertThat(out.resolve("Old.json")).doesNotExist();
  }

  @Test
  void dropsTheLeastImportantStretchesFirstWhenOverThePointLimit() {
    // 3 stretches of 1,500 points: 4,500 is over 4,000, so one goes: the highest scalerank.
    Kept important = new Kept(run(2, 1, 1_500), 100);
    Kept middling = new Kept(run(5, 2, 1_500), 100);
    Kept least = new Kept(run(9, 3, 1_500), 100);

    List<Kept> kept = CountryRiverLimits.withinPointLimit(List.of(important, middling, least));

    assertThat(kept).containsExactly(important, middling);
  }

  @Test
  void dropsTheShortestOfTheSameScalerankFirst() {
    Kept longer = new Kept(run(9, 1, 2_100), 300);
    Kept shorter = new Kept(run(9, 2, 2_100), 100);

    assertThat(CountryRiverLimits.withinPointLimit(List.of(longer, shorter)))
        .containsExactly(longer);
  }

  @Test
  void keepsEverythingWithinThePointLimit() {
    List<Kept> kept = List.of(new Kept(run(2, 1, 2_000), 100), new Kept(run(9, 2, 2_000), 100));

    assertThat(CountryRiverLimits.withinPointLimit(kept)).isEqualTo(kept);
  }

  private static Lake lake(double area, int points) {
    List<double[]> ring = new ArrayList<>();
    for (int i = 0; i < points; i++) {
      ring.add(new double[] {i, i});
    }
    return new Lake(area, List.of(List.of(ring)));
  }

  @Test
  void keepsTheLargestLakesThatFitAndSkipsOneTooBigToFit() {
    Lake huge = lake(900, 1_500);
    Lake big = lake(500, 1_000); // does not fit after huge
    Lake small = lake(100, 400);

    assertThat(CountryLakes.withinLakeLimit(List.of(small, big, huge)))
        .containsExactly(huge, small);
  }

  @Test
  void simplifiesALakeWithTheCountrysToleranceAndDropsARingLeftWithTooFewPoints() {
    List<double[]> outer =
        List.of(
            new double[] {0, 0},
            new double[] {5, 0.001},
            new double[] {10, 0},
            new double[] {10, 10},
            new double[] {0, 10},
            new double[] {0, 0});
    List<double[]> sliver =
        List.of(
            new double[] {2, 2},
            new double[] {2.001, 2},
            new double[] {2, 2.001},
            new double[] {2, 2});
    List<double[]> tiny =
        List.of(
            new double[] {0, 0},
            new double[] {0.0001, 0},
            new double[] {0.0001, 0.0001},
            new double[] {0, 0});

    List<List<List<double[]>>> result =
        CountryLakes.simplifyLake(List.of(List.of(outer, sliver), List.of(tiny)), 0.05);

    assertThat(result).hasSize(1); // the tiny polygon's outer ring has too few points left
    assertThat(result.get(0).get(0)).hasSize(5); // the near-straight point on the bottom edge goes
  }

  @Test
  void theSummaryShowsTheDistributionTheCountsAndTheEligibleCountries() throws Exception {
    String output =
        CapturedOutput.of(
            () ->
                CountryRiversMain.main(
                    new String[] {
                      rivers.toString(),
                      wikidata.toString(),
                      overrides.toString(),
                      lakes.toString(),
                      countries.toString(),
                      out.toString()
                    }));

    assertThat(output)
        .contains("Source river length by scalerank")
        .contains("Major: scalerank up to 4")
        .contains("Written: 2 countries")
        .contains("Most river points:")
        .contains("Eligible (at least 5 rivers and 1000 km): 0");
  }
}
