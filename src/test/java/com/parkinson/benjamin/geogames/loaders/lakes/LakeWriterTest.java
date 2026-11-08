package com.parkinson.benjamin.geogames.loaders.lakes;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.model.Lake;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LakeWriterTest {

  @TempDir Path directory;

  private static final List<double[]> RING_A =
      List.of(new double[] {0, 0}, new double[] {1, 0}, new double[] {1, 1.5}, new double[] {0, 0});
  private static final List<double[]> RING_B =
      List.of(
          new double[] {5, 5}, new double[] {6, 5}, new double[] {6, 6.25}, new double[] {5, 5});

  private static LakeCandidate candidate(
      String qid, String name, List<List<List<double[]>>> polygons) {
    return new LakeCandidate(qid, name, List.of("Alias"), "Africa", 10, false, polygons, 500);
  }

  private static LakeCandidate singlePolygon(String qid, String name) {
    return candidate(qid, name, List.of(List.of(RING_A)));
  }

  private static Map<String, List<String>> countriesFor(String... qids) {
    Map<String, List<String>> map = new HashMap<>();
    for (String qid : qids) {
      map.put(qid, List.of("Kenya", "Uganda"));
    }
    return map;
  }

  private Lake read(Path file) throws IOException {
    return GeoJson.lenientReader().readValue(file.toFile(), Lake.class);
  }

  @Test
  void clearsJsonFilesButLeavesOtherFiles() throws IOException {
    Files.writeString(directory.resolve("old-Q9.json"), "{}");
    Files.writeString(directory.resolve("notes.txt"), "keep");

    new LakeWriter().write(List.of(singlePolygon("Q1", "Victoria")), countriesFor("Q1"), directory);

    assertThat(directory.resolve("old-Q9.json")).doesNotExist();
    assertThat(directory.resolve("notes.txt")).exists();
    assertThat(directory.resolve("victoria-Q1.json")).exists();
  }

  @Test
  void createsTheOutputDirectoryWhenMissing() throws IOException {
    Path nested = directory.resolve("a").resolve("b");

    new LakeWriter().write(List.of(singlePolygon("Q1", "Victoria")), countriesFor("Q1"), nested);

    assertThat(nested.resolve("victoria-Q1.json")).exists();
  }

  @Test
  void fileNameIsTheSlugAndTheQid() {
    assertThat(LakeWriter.fileName(singlePolygon("Q5", "Lac Léman")))
        .isEqualTo("lac-leman-Q5.json");
    assertThat(LakeWriter.fileName(singlePolygon("Q6", "Lake St. Clair")))
        .isEqualTo("lake-st-clair-Q6.json");
  }

  @Test
  void aNameWithNothingSluggableFallsBackToLake() {
    assertThat(LakeWriter.fileName(singlePolygon("Q7", "バイカル湖"))).isEqualTo("lake-Q7.json");
  }

  @Test
  void eachFileReadsBackAsTheSameLakeAndEndsWithANewline() throws IOException {
    LakeCandidate candidate = singlePolygon("Q1", "Victoria");
    List<Path> written = new LakeWriter().write(List.of(candidate), countriesFor("Q1"), directory);

    assertThat(written).containsExactly(directory.resolve("victoria-Q1.json"));
    assertThat(read(written.get(0)))
        .isEqualTo(LakeWriter.toLake(candidate, List.of("Kenya", "Uganda")));
    assertThat(Files.readString(written.get(0))).endsWith("}\n");
  }

  @Test
  void aSinglePolygonIsWrittenAsAPolygon() {
    Lake lake = LakeWriter.toLake(singlePolygon("Q1", "Victoria"), List.of("Kenya"));

    assertThat(lake.geoData().geometry().type()).isEqualTo("Polygon");
    assertThat(lake.geoData().geometry().coordinates())
        .isEqualTo(
            List.of(
                List.of(
                    List.of(0.0, 0.0), List.of(1.0, 0.0), List.of(1.0, 1.5), List.of(0.0, 0.0))));
    assertThat(lake.countries()).containsExactly("Kenya");
    assertThat(lake.additionalNames()).containsExactly("Alias");
    assertThat(lake.continent()).isEqualTo("Africa");
  }

  @Test
  void severalPolygonsAreWrittenAsAMultiPolygon() {
    LakeCandidate two = candidate("Q2", "Aral", List.of(List.of(RING_A), List.of(RING_B)));

    Lake lake = LakeWriter.toLake(two, List.of());

    assertThat(lake.geoData().geometry().type()).isEqualTo("MultiPolygon");
    assertThat(lake.geoData().geometry().coordinates()).hasSize(2);
  }

  @Test
  void aPolygonWithAHoleKeepsBothRings() {
    LakeCandidate withIsland = candidate("Q3", "Island Lake", List.of(List.of(RING_A, RING_B)));

    Lake lake = LakeWriter.toLake(withIsland, List.of());

    assertThat(lake.geoData().geometry().type()).isEqualTo("Polygon");
    assertThat(lake.geoData().geometry().coordinates()).hasSize(2);
  }

  @Test
  void writingTheSameInputTwiceGivesIdenticalFiles() throws IOException {
    List<LakeCandidate> lakes =
        List.of(singlePolygon("Q1", "Victoria"), singlePolygon("Q2", "Tanganyika"));
    Map<String, List<String>> countries = countriesFor("Q1", "Q2");
    Path first = directory.resolve("first");
    Path second = directory.resolve("second");

    new LakeWriter().write(lakes, countries, first);
    new LakeWriter().write(lakes, countries, second);

    for (String file : List.of("victoria-Q1.json", "tanganyika-Q2.json")) {
      assertThat(Files.readAllBytes(first.resolve(file)))
          .isEqualTo(Files.readAllBytes(second.resolve(file)));
    }
  }
}
