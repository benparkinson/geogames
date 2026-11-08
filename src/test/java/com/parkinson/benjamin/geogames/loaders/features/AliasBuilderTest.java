package com.parkinson.benjamin.geogames.loaders.features;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.loaders.rivers.RiverAssembler;
import java.util.List;
import org.junit.jupiter.api.Test;

class AliasBuilderTest {

  private record TestOverride(
      Boolean exclude,
      String name,
      String continent,
      List<String> aliases,
      List<String> addAliases,
      List<String> removeAliases)
      implements AliasBuilder.NamedOverride {}

  private static AliasBuilder.NamedOverride override(
      List<String> aliases, List<String> add, List<String> remove) {
    return new TestOverride(null, null, null, aliases, add, remove);
  }

  private static List<String> build(
      String name,
      List<String> wikidata,
      String neName,
      String neNameEn,
      AliasBuilder.NamedOverride override) {
    return AliasBuilder.build(
        name, wikidata, neName, neNameEn, override, RiverAssembler::riverVariants);
  }

  @Test
  void murrayRiverGivesMurray() {
    assertThat(build("Murray River", List.of(), null, null, null)).containsExactly("Murray");
  }

  @Test
  void aNameWithoutRiverGainsRiver() {
    assertThat(build("Danube", List.of(), null, null, null)).containsExactly("Danube River");
  }

  @Test
  void aCyrillicAliasIsDropped() {
    assertThat(build("Volga", List.of("Волга", "Itil"), null, null, null))
        .containsExactly("Itil", "Volga River");
  }

  @Test
  void aDuplicateByNormalisationIsDropped() {
    List<String> aliases = build("Rhone", List.of("Rhône", "RHONE", "Rhodanus"), null, null, null);

    assertThat(aliases).containsExactly("Rhodanus", "Rhone River");
  }

  @Test
  void theNameIsNeverInItsOwnAliases() {
    List<String> aliases = build("Rhine", List.of("rhine", "Rhin"), "Rhine", "Rhine", null);

    assertThat(aliases.stream().map(AliasBuilder::normalise)).doesNotContain("rhine");
  }

  @Test
  void aliasesWithDigitsOrTooLongAreDropped() {
    String longAlias = "A".repeat(41);

    assertThat(build("Nile", List.of("Nile 2", longAlias), null, null, null))
        .containsExactly("Nile River");
  }

  @Test
  void addAndRemoveAdjustTheDerivedList() {
    AliasBuilder.NamedOverride override =
        override(null, List.of("Huang He"), List.of("yellow river variant", "Yellow"));

    List<String> aliases =
        build("Yellow River", List.of("Yellow", "Mother River"), null, null, override);

    assertThat(aliases).containsExactly("Mother River", "Huang He");
  }

  @Test
  void aliasesInAnOverrideReplaceEverything() {
    AliasBuilder.NamedOverride override = override(List.of("Only This"), List.of("Ignored"), null);

    assertThat(build("Murray River", List.of("Wikidata"), "Murray", null, override))
        .containsExactly("Only This");
  }
}
