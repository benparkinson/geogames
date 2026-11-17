package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import org.junit.jupiter.api.Test;

class CountryRiversEligibilityTest {

  private static boolean eligible(int riverCount, long riverKm) {
    return CountryRiversEligibility.countryRivers()
        .test(new CountryRiversIndexEntry("X", "X.json", riverCount, riverKm, 0));
  }

  @Test
  void aCountryNeedsFiveRiversAndAThousandKilometresOfThem() {
    assertThat(CountryRiversEligibility.MIN_RIVERS).isEqualTo(5);
    assertThat(CountryRiversEligibility.MIN_RIVER_KM).isEqualTo(1_000);

    assertThat(eligible(5, 1_000)).isTrue();
    assertThat(eligible(4, 5_000)).isFalse();
    assertThat(eligible(30, 999)).isFalse();
  }
}
