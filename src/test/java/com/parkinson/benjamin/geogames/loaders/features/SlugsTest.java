package com.parkinson.benjamin.geogames.loaders.features;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugsTest {

  @Test
  void lowerCasesAndJoinsWordsWithHyphens() {
    assertThat(Slugs.slug("Great Bear Lake", "x")).isEqualTo("great-bear-lake");
  }

  @Test
  void stripsAccentsInsteadOfDroppingTheLetters() {
    assertThat(Slugs.slug("Río Paraná", "x")).isEqualTo("rio-parana");
  }

  @Test
  void collapsesPunctuationAndTrimsHyphensFromTheEnds() {
    assertThat(Slugs.slug("  (St. John's) -- River!  ", "x")).isEqualTo("st-john-s-river");
  }

  @Test
  void aNameWithNoLatinLettersFallsBack() {
    assertThat(Slugs.slug("長江", "river")).isEqualTo("river");
    assertThat(Slugs.slug("", "lake")).isEqualTo("lake");
  }
}
