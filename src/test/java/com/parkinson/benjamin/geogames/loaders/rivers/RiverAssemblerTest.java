package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthFeature;
import com.parkinson.benjamin.geogames.loaders.features.WikidataEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RiverAssemblerTest {

  /** About 333 km along the equator: over the 200 km minimum. */
  private static final List<List<double[]>> LONG_LINE =
      List.of(List.of(new double[] {0, 0}, new double[] {3, 0}));

  /** About 111 km: under the minimum. */
  private static final List<List<double[]>> SHORT_LINE =
      List.of(List.of(new double[] {0, 0}, new double[] {1, 0}));

  private static NaturalEarthFeature group(
      String qid, String name, String nameEn, List<List<double[]>> lines) {
    return new NaturalEarthFeature(qid, name, nameEn, lines, List.of());
  }

  private static WikidataEntry entry(String label, int sitelinks, String continent) {
    return new WikidataEntry(label, sitelinks, List.of(), List.of(continent), List.of());
  }

  private static RiverOverride override(
      Boolean exclude, String name, String continent, Boolean mainPieceOnly) {
    return new RiverOverride(
        exclude, name, continent, null, null, null, mainPieceOnly, null, null, null);
  }

  private static RiverAssembler.Assembly assemble(
      Map<String, NaturalEarthFeature> groups,
      Map<String, WikidataEntry> wikidata,
      Map<String, RiverOverride> overrides) {
    return new RiverAssembler().assemble(groups, wikidata, overrides);
  }

  @Test
  void aRiverExcludedByAnOverrideIsDroppedEvenWithWikidata() {
    var assembly =
        assemble(
            Map.of("Q1", group("Q1", "Gone", "Gone", LONG_LINE)),
            Map.of("Q1", entry("Gone", 5, "Asia")),
            Map.of("Q1", override(true, null, null, null)));

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped()).containsEntry("excluded by override", 1);
  }

  @Test
  void aRiverWithNoWikidataEntryAndNoOverrideIsDropped() {
    var assembly =
        assemble(Map.of("Q1", group("Q1", "Lost", "Lost", LONG_LINE)), Map.of(), Map.of());

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped()).containsEntry("no Wikidata entry", 1);
  }

  @Test
  void anOverrideKeepsARiverThatHasNoWikidataEntry() {
    var assembly =
        assemble(
            Map.of("Q1", group("Q1", "Odd", "Odd", LONG_LINE)),
            Map.of(),
            Map.of("Q1", override(null, null, "Africa", null)));

    assertThat(assembly.candidates())
        .singleElement()
        .satisfies(
            river -> {
              assertThat(river.name()).isEqualTo("Odd");
              assertThat(river.continent()).isEqualTo("Africa");
              assertThat(river.sitelinks()).isZero();
              assertThat(river.fromOverride()).isTrue();
            });
  }

  @Test
  void riversUnder200KmAreDroppedUnlessAnOverrideNamesThem() {
    var assembly =
        assemble(
            Map.of(
                "Q1", group("Q1", "Brook", "Brook", SHORT_LINE),
                "Q2", group("Q2", "Rill", "Rill", SHORT_LINE),
                "Q3", group("Q3", "Big", "Big", LONG_LINE)),
            Map.of(
                "Q1", entry("Brook", 5, "Europe"),
                "Q2", entry("Rill", 5, "Europe"),
                "Q3", entry("Big", 5, "Europe")),
            Map.of("Q2", override(null, null, null, null)));

    assertThat(assembly.candidates())
        .extracting(RiverCandidate::name)
        .containsExactlyInAnyOrder("Rill", "Big");
    assertThat(assembly.dropped()).containsEntry("shorter than 200 km", 1);
  }

  @Test
  void mainPieceOnlyDropsLinesDetachedFromTheLongestPiece() {
    // Two joined lines along the equator (about 666 km) and a short one far to the north.
    List<List<double[]>> lines =
        List.of(
            List.of(new double[] {0, 0}, new double[] {3, 0}),
            List.of(new double[] {0, 10}, new double[] {1, 10}),
            List.of(new double[] {3, 0}, new double[] {6, 0}));

    var kept =
        assemble(
            Map.of("Q1", group("Q1", "Congo", "Congo", lines)),
            Map.of("Q1", entry("Congo", 5, "Africa")),
            Map.of("Q1", override(null, null, null, true)));
    var all =
        assemble(
            Map.of("Q1", group("Q1", "Congo", "Congo", lines)),
            Map.of("Q1", entry("Congo", 5, "Africa")),
            Map.of("Q1", override(null, null, null, null)));

    assertThat(kept.candidates())
        .singleElement()
        .satisfies(
            river -> {
              assertThat(river.lines()).hasSize(2);
              assertThat(river.lines()).noneMatch(line -> line.get(0)[1] == 10);
            });
    assertThat(all.candidates().get(0).lines()).hasSize(3);
  }

  @Test
  void anOverrideWithNoGeometryAtAllIsDropped() {
    var assembly =
        assemble(Map.of(), Map.of(), Map.of("Q9", override(null, "Nothing", "Europe", null)));

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped()).containsEntry("no geometry", 1);
  }

  @Test
  void anExcludedOverrideOnlyRiverIsNotAdded() {
    var assembly = assemble(Map.of(), Map.of(), Map.of("Q9", override(true, null, null, null)));

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped()).isEmpty();
  }

  @Test
  void aRiverWithNoNameAnywhereIsDropped() {
    var assembly =
        assemble(
            Map.of("Q1", group("Q1", " ", null, LONG_LINE)),
            Map.of("Q1", entry("", 5, "Asia")),
            Map.of());

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped()).containsEntry("no name", 1);
  }

  @Test
  void theNameComesFromTheOverrideThenWikidataThenNaturalEarthEnglishThenNaturalEarth() {
    NaturalEarthFeature feature = group("Q1", "Rio Local", "River English", LONG_LINE);
    Map<String, NaturalEarthFeature> groups = Map.of("Q1", feature);

    assertThat(
            assemble(
                    groups,
                    Map.of("Q1", entry("Wiki Name", 1, "Asia")),
                    Map.of("Q1", override(null, "Override Name", null, null)))
                .candidates()
                .get(0)
                .name())
        .isEqualTo("Override Name");
    assertThat(
            assemble(groups, Map.of("Q1", entry("Wiki Name", 1, "Asia")), Map.of())
                .candidates()
                .get(0)
                .name())
        .isEqualTo("Wiki Name");
    assertThat(
            assemble(groups, Map.of("Q1", entry(" ", 1, "Asia")), Map.of())
                .candidates()
                .get(0)
                .name())
        .isEqualTo("River English");
    assertThat(
            assemble(
                    Map.of("Q1", group("Q1", "Rio Local", null, LONG_LINE)),
                    Map.of("Q1", entry(null, 1, "Asia")),
                    Map.of())
                .candidates()
                .get(0)
                .name())
        .isEqualTo("Rio Local");
  }

  @Test
  void anOverrideRiverWithNoContinentIsDroppedButOtherwiseTheContinentStaysNull() {
    var lost = new WikidataEntry("Lost", 5, List.of(), List.of(), List.of());

    var withOverride =
        assemble(
            Map.of("Q1", group("Q1", "Lost", "Lost", LONG_LINE)),
            Map.of("Q1", lost),
            Map.of("Q1", override(null, null, null, null)));
    var withoutOverride =
        assemble(
            Map.of("Q1", group("Q1", "Lost", "Lost", LONG_LINE)), Map.of("Q1", lost), Map.of());

    assertThat(withOverride.candidates()).isEmpty();
    assertThat(withOverride.dropped()).containsEntry("override river with no continent", 1);
    assertThat(withoutOverride.candidates())
        .singleElement()
        .satisfies(river -> assertThat(river.continent()).isNull());
  }

  @Test
  void aliasesIncludeTheRiverVariantAndTheWikidataAliases() {
    var wikidata =
        Map.of(
            "Q1", new WikidataEntry("Danube", 5, List.of("Donau"), List.of("Europe"), List.of()));

    var river =
        assemble(Map.of("Q1", group("Q1", "Danube", "Danube", LONG_LINE)), wikidata, Map.of())
            .candidates()
            .get(0);

    assertThat(river.aliases()).contains("Donau", "Danube River");
  }

  @Test
  void variantsSwapRiverInAndOut() {
    assertThat(RiverAssembler.riverVariants("Murray River")).containsExactly("Murray");
    assertThat(RiverAssembler.riverVariants("Danube")).containsExactly("Danube River");
    assertThat(RiverAssembler.riverVariants("Rio River Grande"))
        .containsExactly("Rio River Grande");
  }
}
