package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverAssembler.Assembly;
import com.parkinson.benjamin.geogames.loaders.rivers.RiverSelector.Selection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RiverSelectionTest {

  /** About 333 km: long enough to pass the 200 km filter. */
  private static final List<List<double[]>> LONG_LINES =
      List.of(List.of(new double[] {0, 0}, new double[] {0, 3}));

  private static NaturalEarthRiver group(String qid, String name) {
    return new NaturalEarthRiver(qid, name, name, LONG_LINES);
  }

  private static WikidataEntry entry(String label, int sitelinks, String continent) {
    return new WikidataEntry(label, sitelinks, List.of(), List.of(continent), List.of());
  }

  private static RiverOverride override(boolean exclude, String name, String continent) {
    return new RiverOverride(exclude, name, continent, null, null, null, null);
  }

  private static Assembly assemble(
      Map<String, NaturalEarthRiver> groups,
      Map<String, WikidataEntry> wikidata,
      Map<String, RiverOverride> overrides) {
    return new RiverAssembler(file -> LONG_LINES).assemble(groups, wikidata, overrides);
  }

  @Test
  void twoFeaturesWithTheSameQidBecomeOneRiver() {
    String json =
        """
        {"features": [
          {"properties": {"wikidataid": "Q1", "name": "", "name_en": "Rhine"},
           "geometry": {"type": "LineString", "coordinates": [[0,0],[0,1]]}},
          {"properties": {"wikidataid": "Q1", "name": "Rhein", "name_en": "Rhine"},
           "geometry": {"type": "MultiLineString", "coordinates": [[[0,1],[0,2]],[[1,1],[1,2]]]}},
          {"properties": {"wikidataid": null, "name": "Nameless"},
           "geometry": {"type": "LineString", "coordinates": [[5,5],[6,6]]}},
          {"properties": {"wikidataid": "Q9", "featurecla": "Canal", "name": "Canal"},
           "geometry": {"type": "LineString", "coordinates": [[5,5],[6,6]]}}
        ]}
        """;

    Map<String, NaturalEarthRiver> groups =
        new NaturalEarthReader().group(GeoJson.lenientReader().readTree(json));

    assertThat(groups).containsOnlyKeys("Q1");
    assertThat(groups.get("Q1").lines()).hasSize(3);
    assertThat(groups.get("Q1").name()).isEqualTo("Rhein");
    assertThat(groups.get("Q1").nameEn()).isEqualTo("Rhine");
  }

  @Test
  void theTopNIsTakenBySitelinks() {
    Map<String, NaturalEarthRiver> groups =
        Map.of("Q1", group("Q1", "A"), "Q2", group("Q2", "B"), "Q3", group("Q3", "C"));
    Map<String, WikidataEntry> wikidata =
        Map.of(
            "Q1",
            entry("A", 10, "Asia"),
            "Q2",
            entry("B", 30, "Asia"),
            "Q3",
            entry("C", 20, "Asia"));

    Selection selection =
        RiverSelector.select(assemble(groups, wikidata, Map.of()).candidates(), 2);

    assertThat(selection.selected())
        .extracting(RiverCandidate::name)
        .containsExactlyInAnyOrder("B", "C");
    assertThat(selection.beyondLimit()).isEqualTo(1);
  }

  @Test
  void overrideRiversAreAlwaysIncludedAndExcludedOnesNever() {
    Map<String, NaturalEarthRiver> groups =
        Map.of(
            "Q1", group("Q1", "Famous"), "Q2", group("Q2", "Banned"), "Q3", group("Q3", "Obscure"));
    Map<String, WikidataEntry> wikidata =
        Map.of(
            "Q1", entry("Famous", 100, "Asia"),
            "Q2", entry("Banned", 500, "Asia"),
            "Q3", entry("Obscure", 1, "Asia"));
    Map<String, RiverOverride> overrides =
        Map.of("Q2", override(true, null, null), "Q3", override(false, null, null));

    Selection selection =
        RiverSelector.select(assemble(groups, wikidata, overrides).candidates(), 2);

    assertThat(selection.selected())
        .extracting(RiverCandidate::name)
        .containsExactlyInAnyOrder("Famous", "Obscure");
  }

  @Test
  void anOverrideWithOnlyAnOsmFileNeedsNothingElse() {
    Map<String, RiverOverride> overrides =
        Map.of(
            "Q7",
            new RiverOverride(
                null, "Murray River", "Australia", List.of("Murray"), null, null, "m.json"));

    Assembly assembly = assemble(Map.of(), Map.of(), overrides);

    assertThat(assembly.candidates())
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.name()).isEqualTo("Murray River");
              assertThat(c.continent()).isEqualTo("Australia");
              assertThat(c.aliases()).containsExactly("Murray");
            });
  }

  @Test
  void shortNonOverrideRiversAreDroppedAndUnresolvedOnesAreSkippedAndRecorded() {
    NaturalEarthRiver tiny =
        new NaturalEarthRiver(
            "Q1", "Tiny", "Tiny", List.of(List.of(new double[] {0, 0}, new double[] {0, 0.5})));
    Map<String, NaturalEarthRiver> groups = Map.of("Q1", tiny, "Q2", group("Q2", "Lost"));
    Map<String, WikidataEntry> wikidata =
        Map.of(
            "Q1",
            entry("Tiny", 99, "Asia"),
            "Q2",
            new WikidataEntry("Lost", 50, List.of(), List.of("Europe", "Asia"), List.of()));

    Assembly assembly = assemble(groups, wikidata, Map.of());
    Selection selection = RiverSelector.select(assembly.candidates(), 10);

    assertThat(assembly.dropped()).containsEntry("shorter than 200 km", 1);
    assertThat(selection.selected()).isEmpty();
    assertThat(selection.unresolved()).extracting(RiverCandidate::name).containsExactly("Lost");
  }

  @Test
  void slugStripsAccentsAndPunctuation() {
    assertThat(RiverWriter.slug("Rhône")).isEqualTo("rhone");
    assertThat(RiverWriter.slug("Rio Grande")).isEqualTo("rio-grande");
    assertThat(RiverWriter.slug("São Francisco River!")).isEqualTo("sao-francisco-river");
  }
}
