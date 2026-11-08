package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.River;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RiverWriterTest {

  @TempDir Path directory;

  private static RiverCandidate candidate(String qid, String name) {
    return new RiverCandidate(
        qid,
        name,
        List.of("Alias"),
        "Europe",
        10,
        false,
        List.of(
            List.of(new double[] {0, 0}, new double[] {1, 1.5}),
            List.of(new double[] {2, 2}, new double[] {3, 3.25}, new double[] {4, 4})),
        100);
  }

  private static RiverFacts facts() {
    return new RiverFacts(
        new Coordinate(0, 0), new Coordinate(4, 4), 1.0, List.of("France", "Spain"));
  }

  private static Map<String, RiverFacts> factsFor(String... qids) {
    Map<String, RiverFacts> map = new java.util.HashMap<>();
    for (String qid : qids) {
      map.put(qid, facts());
    }
    return map;
  }

  private River read(Path file) throws IOException {
    return GeoJson.lenientReader().readValue(file.toFile(), River.class);
  }

  @Test
  void clearsJsonFilesButLeavesOtherFiles() throws IOException {
    Files.writeString(directory.resolve("old-Q9.json"), "{}");
    Files.writeString(directory.resolve("notes.txt"), "keep");

    new RiverWriter().write(List.of(candidate("Q1", "Rhine")), factsFor("Q1"), directory);

    assertThat(directory.resolve("old-Q9.json")).doesNotExist();
    assertThat(directory.resolve("notes.txt")).exists();
    assertThat(directory.resolve("rhine-Q1.json")).exists();
  }

  @Test
  void createsTheOutputDirectoryWhenMissing() throws IOException {
    Path nested = directory.resolve("a").resolve("b");

    new RiverWriter().write(List.of(candidate("Q1", "Rhine")), factsFor("Q1"), nested);

    assertThat(nested.resolve("rhine-Q1.json")).exists();
  }

  @Test
  void fileNameIsTheSlugAndTheQid() {
    assertThat(RiverWriter.fileName(candidate("Q5", "Río de la Plata")))
        .isEqualTo("rio-de-la-plata-Q5.json");
    assertThat(RiverWriter.fileName(candidate("Q6", "St. John's River")))
        .isEqualTo("st-john-s-river-Q6.json");
  }

  @Test
  void aNameWithNothingSluggableFallsBackToRiver() {
    assertThat(RiverWriter.fileName(candidate("Q7", "長江"))).isEqualTo("river-Q7.json");
  }

  @Test
  void eachFileReadsBackAsTheSameRiverAndEndsWithANewline() throws IOException {
    RiverCandidate candidate = candidate("Q1", "Rhine");
    List<Path> written = new RiverWriter().write(List.of(candidate), factsFor("Q1"), directory);

    assertThat(written).containsExactly(directory.resolve("rhine-Q1.json"));
    assertThat(read(written.get(0))).isEqualTo(RiverWriter.toRiver(candidate, facts()));
    assertThat(Files.readString(written.get(0))).endsWith("}\n");
  }

  @Test
  void toRiverMakesOneMultiLineStringLinePerCandidateLine() {
    River river = RiverWriter.toRiver(candidate("Q1", "Rhine"), facts());

    assertThat(river.geoData().geometry().type()).isEqualTo("MultiLineString");
    assertThat(river.geoData().geometry().coordinates())
        .isEqualTo(
            List.of(
                List.of(List.of(0.0, 0.0), List.of(1.0, 1.5)),
                List.of(List.of(2.0, 2.0), List.of(3.0, 3.25), List.of(4.0, 4.0))));
    assertThat(river.id()).isEqualTo("Q1");
    assertThat(river.additionalNames()).containsExactly("Alias");
    assertThat(river.countries()).containsExactly("France", "Spain");
    assertThat(river.source()).isEqualTo(new Coordinate(0, 0));
    assertThat(river.mouth()).isEqualTo(new Coordinate(4, 4));
  }

  @Test
  void writingTheSameInputTwiceGivesIdenticalFiles() throws IOException {
    List<RiverCandidate> rivers = List.of(candidate("Q1", "Rhine"), candidate("Q2", "Danube"));
    Map<String, RiverFacts> facts = factsFor("Q1", "Q2");
    Path first = directory.resolve("first");
    Path second = directory.resolve("second");

    new RiverWriter().write(rivers, facts, first);
    new RiverWriter().write(rivers, facts, second);

    for (String file : List.of("rhine-Q1.json", "danube-Q2.json")) {
      assertThat(Files.readAllBytes(first.resolve(file)))
          .isEqualTo(Files.readAllBytes(second.resolve(file)));
    }
  }
}
