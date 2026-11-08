package com.parkinson.benjamin.geogames.loaders.countries;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.loaders.CapturedOutput;
import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CountryLoaderMainTest {

  @TempDir Path out;

  private Path fixtures;

  @BeforeEach
  void locateFixtures() throws URISyntaxException {
    fixtures = Path.of(getClass().getResource("/fixtures/loaders/countries").toURI());
  }

  private CountryLoaderMain.Report run(Path outDir) throws IOException {
    return CountryLoaderMain.run(
        fixtures.resolve("countries.geo.json"),
        fixtures.resolve("additionalNames.json"),
        fixtures.resolve("excludedNames.json"),
        outDir);
  }

  private List<String> fileNames(Path dir) throws IOException {
    try (Stream<Path> files = Files.list(dir)) {
      return files.map(p -> p.getFileName().toString()).sorted().toList();
    }
  }

  @Test
  void writesEveryCountryExceptTheExcludedOnes() throws IOException {
    CountryLoaderMain.Report report = run(out);

    assertThat(report.countries()).extracting(Country::name).containsExactly("Alpha Land", "Beta");
    assertThat(fileNames(out)).containsExactly("AlphaLand.json", "Beta.json");
  }

  @Test
  void aliasesComeFromTheAdditionalNamesFile() throws IOException {
    run(out);

    List<Country> read = new ProcessedCountryReader().readCountries(out);

    assertThat(read)
        .filteredOn(c -> c.name().equals("Alpha Land"))
        .singleElement()
        .satisfies(c -> assertThat(c.additionalNames()).containsExactly("Alpha", "Alphaland"));
    assertThat(read)
        .filteredOn(c -> c.name().equals("Beta"))
        .singleElement()
        .satisfies(c -> assertThat(c.additionalNames()).isEmpty());
  }

  @Test
  void whatIsWrittenReadsBackAsTheReportedCountries() throws IOException {
    CountryLoaderMain.Report report = run(out);

    assertThat(new ProcessedCountryReader().readCountries(out))
        .containsExactlyInAnyOrderElementsOf(report.countries());
  }

  @Test
  void clearsOldJsonFilesButLeavesOtherFiles() throws IOException {
    Files.writeString(out.resolve("Old.json"), "{}");
    Files.writeString(out.resolve("notes.txt"), "keep");

    run(out);

    assertThat(fileNames(out)).doesNotContain("Old.json").contains("notes.txt");
  }

  @Test
  void runningTwiceGivesIdenticalFiles() throws IOException {
    Path first = Files.createDirectory(out.resolve("first"));
    Path second = Files.createDirectory(out.resolve("second"));

    run(first);
    run(second);

    for (String name : fileNames(first)) {
      assertThat(Files.readAllBytes(first.resolve(name)))
          .isEqualTo(Files.readAllBytes(second.resolve(name)));
    }
  }

  @Test
  void mainWritesTheFilesAndPrintsNothing() throws Exception {
    String printed =
        CapturedOutput.of(
            () ->
                CountryLoaderMain.main(
                    new String[] {
                      fixtures.resolve("countries.geo.json").toString(),
                      fixtures.resolve("additionalNames.json").toString(),
                      fixtures.resolve("excludedNames.json").toString(),
                      out.toString()
                    }));

    assertThat(printed).isEmpty();
    assertThat(fileNames(out)).containsExactly("AlphaLand.json", "Beta.json");
  }
}
