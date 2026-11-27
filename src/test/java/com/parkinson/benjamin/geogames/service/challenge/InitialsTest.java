package com.parkinson.benjamin.geogames.service.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InitialsTest {

  private final Initials initials = new Initials(new String[0]);

  @Test
  void initialsAreTrimmedAndUpperCased() {
    assertThat(initials.normalise(" abc ")).isEqualTo("ABC");
    assertThat(initials.normalise("q")).isEqualTo("Q");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "ABCD", "A1", "A B", "A.", "\u00c9", "\u00df"})
  void anythingButOneToThreeLettersIsRejected(String text) {
    assertThatThrownBy(() -> initials.normalise(text)).isInstanceOf(BadRequestException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ass", "FUK", "fgt", " kkk "})
  void blockedWordsAreRejected(String text) {
    assertThatThrownBy(() -> initials.normalise(text)).isInstanceOf(BadRequestException.class);
  }

  @Test
  void extraWordsAreBlockedAndHiddenWhereAlreadyStored() {
    Initials extended = new Initials(new String[] {" abc", "XY "});

    for (String word : new String[] {"ABC", "xy", "ASS"}) {
      assertThatThrownBy(() -> extended.normalise(word)).isInstanceOf(BadRequestException.class);
    }
    assertThat(extended.visible("ABC")).isNull();
    assertThat(extended.visible("XY")).isNull();
    assertThat(extended.visible("QRS")).isEqualTo("QRS");
    assertThat(extended.visible(null)).isNull();
  }
}
