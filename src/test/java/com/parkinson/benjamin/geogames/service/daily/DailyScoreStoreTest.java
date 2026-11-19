package com.parkinson.benjamin.geogames.service.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DailyScoreStoreTest {

  @ParameterizedTest
  @CsvSource({"0,0", "49,0", "50,1", "449,8", "450,9", "499,9", "500,10"})
  void aScoreFallsInATenthOfTheMaximum(int score, int bucket) {
    assertThat(DailyScoreStore.bucket(score, 500)).isEqualTo(bucket);
  }

  @ParameterizedTest
  @CsvSource({"-1,500", "501,500", "0,0", "5,-5"})
  void aScoreOutsideTheRangeIsRejected(int score, int max) {
    assertThatThrownBy(() -> DailyScoreStore.bucket(score, max))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
