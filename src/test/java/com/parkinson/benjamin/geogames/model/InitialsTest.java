package com.parkinson.benjamin.geogames.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InitialsTest {

  @Test
  void initialsAreTrimmedAndUpperCased() {
    assertThat(Initials.normalise(" abc ")).isEqualTo("ABC");
    assertThat(Initials.normalise("q")).isEqualTo("Q");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "ABCD", "A1", "A B", "A.", "\u00c9", "\u00df"})
  void anythingButOneToThreeLettersIsRejected(String initials) {
    assertThatThrownBy(() -> Initials.normalise(initials)).isInstanceOf(BadRequestException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ass", "FUK", " kkk "})
  void blockedWordsAreRejected(String initials) {
    assertThatThrownBy(() -> Initials.normalise(initials)).isInstanceOf(BadRequestException.class);
  }
}
