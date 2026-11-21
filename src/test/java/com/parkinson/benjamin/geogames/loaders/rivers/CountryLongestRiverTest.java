package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.loaders.rivers.RiverClipper.Run;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CountryLongestRiverTest {

  /** A line along the equator: one degree of longitude is about 111 km. */
  private static Run run(String qid, double degrees) {
    List<double[]> points = new ArrayList<>();
    points.add(new double[] {0, 0});
    points.add(new double[] {degrees, 0});
    return new Run(3, 1, qid, points);
  }

  private static Map<String, RiverCandidate> candidates(String... namesByQid) {
    Map<String, RiverCandidate> map = new LinkedHashMap<>();
    for (int i = 0; i < namesByQid.length; i += 2) {
      map.put(
          namesByQid[i],
          new RiverCandidate(
              namesByQid[i], namesByQid[i + 1], List.of(), "Europe", 10, false, List.of(), 500));
    }
    return map;
  }

  private static RiverCandidate withAliases(String qid, String name, String... aliases) {
    return new RiverCandidate(qid, name, List.of(aliases), "Africa", 10, false, List.of(), 500);
  }

  @Test
  void theLongestStretchInTheCountryWins() {
    // The Danube is longer overall, but the Tisza has more of it in this country.
    Map<String, RiverCandidate> candidates = candidates("Q1", "Danube", "Q2", "Tisza");

    assertThat(
            CountryLongestRiver.of(
                List.of("Hungary"), List.of(run("Q1", 2), run("Q2", 5)), candidates))
        .contains("Tisza");
  }

  @Test
  void stretchesOfTheSameRiverAreAdded() {
    Map<String, RiverCandidate> candidates = candidates("Q1", "Danube", "Q2", "Tisza");

    assertThat(
            CountryLongestRiver.of(
                List.of("Hungary"), List.of(run("Q1", 2), run("Q2", 3), run("Q1", 2)), candidates))
        .contains("Danube");
  }

  @Test
  void runsWithNoQidOrAnUnknownQidAreIgnored() {
    Map<String, RiverCandidate> candidates = candidates("Q1", "Danube");

    assertThat(
            CountryLongestRiver.of(
                List.of("Hungary"), List.of(run(null, 9), run("Q99", 8), run("Q1", 2)), candidates))
        .contains("Danube");
    assertThat(CountryLongestRiver.of(List.of("Hungary"), List.of(run(null, 9)), candidates))
        .isEmpty();
  }

  @Test
  void aRiverNamedAfterTheCountryIsSkipped() {
    Map<String, RiverCandidate> candidates = candidates("Q1", "Niger", "Q2", "Benue");

    assertThat(
            CountryLongestRiver.of(
                List.of("Niger"), List.of(run("Q1", 9), run("Q2", 3)), candidates))
        .contains("Benue");
  }

  @Test
  void aRiverWithTheCountrysAliasInItsNameOrAliasesIsSkipped() {
    Map<String, RiverCandidate> candidates = new LinkedHashMap<>();
    candidates.put("Q1", withAliases("Q1", "Congo River"));
    candidates.put("Q2", withAliases("Q2", "Zaire", "Congo"));
    candidates.put("Q3", withAliases("Q3", "Ubangi"));

    assertThat(
            CountryLongestRiver.of(
                List.of("Republic of the Congo", "Congo"),
                List.of(run("Q1", 9), run("Q2", 8), run("Q3", 2)),
                candidates))
        .contains("Ubangi");
  }

  @Test
  void aRiverWhoseNameIsPartOfTheCountrysNameIsSkipped() {
    Map<String, RiverCandidate> candidates = new LinkedHashMap<>();
    candidates.put("Q1", withAliases("Q1", "Congo"));
    candidates.put("Q2", withAliases("Q2", "Kasai"));

    assertThat(
            CountryLongestRiver.of(
                List.of("Democratic Republic of the Congo", "DR Congo"),
                List.of(run("Q1", 9), run("Q2", 2)),
                candidates))
        .contains("Kasai");
  }

  @Test
  void aNameIsMatchedAsAWholeWordIgnoringAccents() {
    Map<String, RiverCandidate> candidates = new LinkedHashMap<>();
    candidates.put("Q1", withAliases("Q1", "Nigeria Creek"));
    candidates.put("Q2", withAliases("Q2", "Río Paraguay"));
    candidates.put("Q3", withAliases("Q3", "Pilcomayo"));

    // "Niger" is not a word of "Nigeria Creek", but "Paraguay" is one of "Río Paraguay".
    assertThat(
            CountryLongestRiver.of(
                List.of("Niger"), List.of(run("Q1", 9), run("Q3", 2)), candidates))
        .contains("Nigeria Creek");
    assertThat(
            CountryLongestRiver.of(
                List.of("Paraguay"), List.of(run("Q2", 9), run("Q3", 2)), candidates))
        .contains("Pilcomayo");
  }

  @Test
  void aRiverWithUnderMinKmInTheCountryIsSkipped() {
    Map<String, RiverCandidate> candidates = candidates("Q1", "Long", "Q2", "Short");
    // 0.4 degrees is about 44 km, under MIN_KM.
    List<Run> runs = List.of(run("Q1", 0.4), run("Q2", 0.3));

    assertThat(CountryLongestRiver.of(List.of("Hungary"), runs, candidates)).isEmpty();
    assertThat(CountryLongestRiver.MIN_KM).isEqualTo(50);
    assertThat(
            CountryLongestRiver.of(
                List.of("Hungary"), List.of(run("Q1", 0.6), run("Q2", 0.3)), candidates))
        .contains("Long");
  }

  @Test
  void tiesGoByName() {
    Map<String, RiverCandidate> candidates = candidates("Q1", "Zeta", "Q2", "Alpha");

    assertThat(
            CountryLongestRiver.of(
                List.of("Hungary"), List.of(run("Q1", 3), run("Q2", 3)), candidates))
        .contains("Alpha");
  }

  @Test
  void noCandidatesGivesNothing() {
    assertThat(CountryLongestRiver.of(List.of("Hungary"), List.of(run("Q1", 5)), Map.of()))
        .isEmpty();
    assertThat(CountryLongestRiver.of(List.of("Hungary"), List.of(), candidates("Q1", "Danube")))
        .isEmpty();
  }
}
