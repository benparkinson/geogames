package com.parkinson.benjamin.geogames.loaders.lakes;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.loaders.features.FeatureSelector;
import com.parkinson.benjamin.geogames.loaders.features.NaturalEarthFeature;
import com.parkinson.benjamin.geogames.loaders.features.WikidataEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LakeAssemblerTest {

  private static List<List<List<double[]>>> square(double size) {
    return List.of(
        List.of(
            List.of(
                new double[] {0, 0},
                new double[] {size, 0},
                new double[] {size, size},
                new double[] {0, size},
                new double[] {0, 0})));
  }

  private static NaturalEarthFeature group(String qid, String name, double size) {
    return new NaturalEarthFeature(qid, name, name, List.of(), square(size));
  }

  private static WikidataEntry entry(String label, int sitelinks, String continent) {
    return new WikidataEntry(label, sitelinks, List.of(), List.of(continent), List.of());
  }

  private static LakeOverride override(Boolean exclude, String name, String continent) {
    return new LakeOverride(exclude, name, continent, null, null, null, null);
  }

  @Test
  void variantsSwapLakeInAndOut() {
    assertThat(LakeAssembler.lakeVariants("Lake Victoria")).containsExactly("Victoria");
    assertThat(LakeAssembler.lakeVariants("Great Bear Lake")).containsExactly("Great Bear");
    assertThat(LakeAssembler.lakeVariants("Titicaca")).containsExactly("Lake Titicaca");
  }

  @Test
  void variantsSkipLowerCaseStartsAndSeas() {
    assertThat(LakeAssembler.lakeVariants("Lake of the Woods")).isEmpty();
    assertThat(LakeAssembler.lakeVariants("Dead Sea")).isEmpty();
    assertThat(LakeAssembler.lakeVariants("Aral Sea")).isEmpty();
  }

  @Test
  void smallLakesAreDroppedUnlessAnOverrideNamesThem() {
    Map<String, NaturalEarthFeature> groups =
        Map.of(
            "Q1",
            group("Q1", "Big", 2),
            "Q2",
            group("Q2", "Tiny", 0.05),
            "Q3",
            group("Q3", "Loch", 0.02));
    Map<String, WikidataEntry> wikidata =
        Map.of(
            "Q1", entry("Big", 10, "Europe"),
            "Q2", entry("Tiny", 9, "Europe"),
            "Q3", entry("Loch", 8, "Europe"));

    LakeAssembler.Assembly assembly =
        new LakeAssembler().assemble(groups, wikidata, Map.of("Q3", override(null, null, null)));

    assertThat(assembly.candidates())
        .extracting(LakeCandidate::name)
        .containsExactlyInAnyOrder("Big", "Loch");
    assertThat(assembly.dropped()).containsEntry("smaller than 100 km²", 1);
  }

  @Test
  void excludedLakesAndLakesWithoutWikidataAreDropped() {
    Map<String, NaturalEarthFeature> groups =
        Map.of("Q1", group("Q1", "Gone", 2), "Q2", group("Q2", "Unknown", 2));

    LakeAssembler.Assembly assembly =
        new LakeAssembler()
            .assemble(
                groups,
                Map.of("Q1", entry("Gone", 5, "Asia")),
                Map.of("Q1", override(true, null, null)));

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped())
        .containsEntry("excluded by override", 1)
        .containsEntry("no Wikidata entry", 1);
  }

  @Test
  void anUnresolvedContinentIsKeptAsNullAndSkippedBySelection() {
    Map<String, NaturalEarthFeature> groups = Map.of("Q1", group("Q1", "Lost", 2));
    Map<String, WikidataEntry> wikidata =
        Map.of(
            "Q1", new WikidataEntry("Lost", 50, List.of(), List.of("Europe", "Asia"), List.of()));

    LakeAssembler.Assembly assembly = new LakeAssembler().assemble(groups, wikidata, Map.of());
    FeatureSelector.Selection<LakeCandidate> selection =
        FeatureSelector.select(assembly.candidates(), Map.of("Asia", 5));

    assertThat(assembly.candidates())
        .singleElement()
        .satisfies(c -> assertThat(c.continent()).isNull());
    assertThat(selection.selected()).isEmpty();
    assertThat(selection.unresolved()).extracting(LakeCandidate::name).containsExactly("Lost");
  }

  @Test
  void anOverrideWithNoNaturalEarthFeatureHasNoGeometryAndIsDropped() {
    LakeAssembler.Assembly assembly =
        new LakeAssembler()
            .assemble(Map.of(), Map.of(), Map.of("Q9", override(null, "Ghost", "Asia")));

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped()).containsEntry("no geometry", 1);
  }

  @Test
  void anExcludedOverrideWithNoFeatureIsNotCounted() {
    LakeAssembler.Assembly assembly =
        new LakeAssembler()
            .assemble(Map.of(), Map.of(), Map.of("Q9", override(true, "Ghost", "Asia")));

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped()).isEmpty();
  }

  @Test
  void anOverrideKeepsALakeWithNoWikidataEntryAndGivesItNoSitelinks() {
    LakeAssembler.Assembly assembly =
        new LakeAssembler()
            .assemble(
                Map.of("Q1", group("Q1", "Odd", 0.02)),
                Map.of(),
                Map.of("Q1", override(null, null, "Oceania")));

    assertThat(assembly.candidates())
        .singleElement()
        .satisfies(
            lake -> {
              assertThat(lake.sitelinks()).isZero();
              assertThat(lake.continent()).isEqualTo("Oceania");
            });
  }

  @Test
  void aLakeWithNoNameAnywhereIsDropped() {
    Map<String, NaturalEarthFeature> groups =
        Map.of("Q1", new NaturalEarthFeature("Q1", " ", null, List.of(), square(2)));

    LakeAssembler.Assembly assembly =
        new LakeAssembler().assemble(groups, Map.of("Q1", entry("", 5, "Asia")), Map.of());

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped()).containsEntry("no name", 1);
  }

  @Test
  void theNameComesFromTheOverrideThenWikidataThenNaturalEarthEnglishThenNaturalEarth() {
    NaturalEarthFeature feature =
        new NaturalEarthFeature("Q1", "Lago Local", "English Lake", List.of(), square(2));
    NaturalEarthFeature noEnglish =
        new NaturalEarthFeature("Q1", "Lago Local", null, List.of(), square(2));
    LakeAssembler assembler = new LakeAssembler();

    assertThat(
            assembler
                .assemble(
                    Map.of("Q1", feature),
                    Map.of("Q1", entry("Wiki Lake", 1, "Asia")),
                    Map.of("Q1", override(null, "Override Lake", null)))
                .candidates()
                .get(0)
                .name())
        .isEqualTo("Override Lake");
    assertThat(
            assembler
                .assemble(
                    Map.of("Q1", feature), Map.of("Q1", entry("Wiki Lake", 1, "Asia")), Map.of())
                .candidates()
                .get(0)
                .name())
        .isEqualTo("Wiki Lake");
    assertThat(
            assembler
                .assemble(Map.of("Q1", feature), Map.of("Q1", entry(" ", 1, "Asia")), Map.of())
                .candidates()
                .get(0)
                .name())
        .isEqualTo("English Lake");
    assertThat(
            assembler
                .assemble(Map.of("Q1", noEnglish), Map.of("Q1", entry(null, 1, "Asia")), Map.of())
                .candidates()
                .get(0)
                .name())
        .isEqualTo("Lago Local");
  }

  @Test
  void anOverrideLakeWithNoContinentIsDropped() {
    WikidataEntry lost = new WikidataEntry("Lost", 5, List.of(), List.of(), List.of());

    LakeAssembler.Assembly assembly =
        new LakeAssembler()
            .assemble(
                Map.of("Q1", group("Q1", "Lost", 2)),
                Map.of("Q1", lost),
                Map.of("Q1", override(null, null, null)));

    assertThat(assembly.candidates()).isEmpty();
    assertThat(assembly.dropped()).containsEntry("override lake with no continent", 1);
  }

  @Test
  void aLakeWithNoPolygonsIsDropped() {
    Map<String, NaturalEarthFeature> groups =
        Map.of("Q1", new NaturalEarthFeature("Q1", "Flat", "Flat", List.of(), List.of()));

    LakeAssembler.Assembly assembly =
        new LakeAssembler().assemble(groups, Map.of("Q1", entry("Flat", 5, "Asia")), Map.of());

    assertThat(assembly.dropped()).containsEntry("no geometry", 1);
  }

  @Test
  void variantsOfABlankRemainderAreSkipped() {
    assertThat(LakeAssembler.lakeVariants("Lake ")).isEmpty();
  }

  @Test
  void theOverrideNameAndAliasesWin() {
    Map<String, NaturalEarthFeature> groups = Map.of("Q1", group("Q1", "Argentino Lake", 2));
    Map<String, WikidataEntry> wikidata = Map.of("Q1", entry("Argentino Lake", 5, "South America"));
    LakeOverride override =
        new LakeOverride(null, "Lake Argentino", null, List.of("Argentino"), null, null, null);

    LakeCandidate lake =
        new LakeAssembler().assemble(groups, wikidata, Map.of("Q1", override)).candidates().get(0);

    assertThat(lake.name()).isEqualTo("Lake Argentino");
    assertThat(lake.aliases()).containsExactly("Argentino");
    assertThat(lake.fromOverride()).isTrue();
  }
}
