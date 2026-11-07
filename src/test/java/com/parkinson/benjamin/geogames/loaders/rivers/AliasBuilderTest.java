package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AliasBuilderTest {

  private static RiverOverride override(
      List<String> aliases, List<String> add, List<String> remove) {
    return new RiverOverride(null, null, null, aliases, add, remove, null, null, null, null);
  }

  @Test
  void murrayRiverGivesMurray() {
    assertThat(AliasBuilder.build("Murray River", List.of(), null, null, null))
        .containsExactly("Murray");
  }

  @Test
  void aNameWithoutRiverGainsRiver() {
    assertThat(AliasBuilder.build("Danube", List.of(), null, null, null))
        .containsExactly("Danube River");
  }

  @Test
  void aCyrillicAliasIsDropped() {
    assertThat(AliasBuilder.build("Volga", List.of("Волга", "Itil"), null, null, null))
        .containsExactly("Itil", "Volga River");
  }

  @Test
  void aDuplicateByNormalisationIsDropped() {
    List<String> aliases =
        AliasBuilder.build("Rhone", List.of("Rhône", "RHONE", "Rhodanus"), null, null, null);

    assertThat(aliases).containsExactly("Rhodanus", "Rhone River");
  }

  @Test
  void theNameIsNeverInItsOwnAliases() {
    List<String> aliases =
        AliasBuilder.build("Rhine", List.of("rhine", "Rhin"), "Rhine", "Rhine", null);

    assertThat(aliases.stream().map(AliasBuilder::normalise)).doesNotContain("rhine");
  }

  @Test
  void aliasesWithDigitsOrTooLongAreDropped() {
    String longAlias = "A".repeat(41);

    assertThat(AliasBuilder.build("Nile", List.of("Nile 2", longAlias), null, null, null))
        .containsExactly("Nile River");
  }

  @Test
  void addAndRemoveAdjustTheDerivedList() {
    RiverOverride override =
        override(null, List.of("Huang He"), List.of("yellow river variant", "Yellow"));

    List<String> aliases =
        AliasBuilder.build("Yellow River", List.of("Yellow", "Mother River"), null, null, override);

    assertThat(aliases).containsExactly("Mother River", "Huang He");
  }

  @Test
  void aliasesInAnOverrideReplaceEverything() {
    RiverOverride override = override(List.of("Only This"), List.of("Ignored"), null);

    assertThat(AliasBuilder.build("Murray River", List.of("Wikidata"), "Murray", null, override))
        .containsExactly("Only This");
  }
}
