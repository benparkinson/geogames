package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.City;
import com.parkinson.benjamin.geogames.model.CountryCities;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CountryCitiesEligibilityTest {

  private static CountryCities with(int count) {
    List<City> cities = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      cities.add(new City("City " + i, 0, 0, 1, false));
    }
    return new CountryCities("X", cities);
  }

  @Test
  void aCountryNeedsAtLeastEightCities() {
    assertThat(CountryCitiesEligibility.MIN_CITIES).isEqualTo(8);
    assertThat(CountryCitiesEligibility.countryCities().test(with(7))).isFalse();
    assertThat(CountryCitiesEligibility.countryCities().test(with(8))).isTrue();
    assertThat(CountryCitiesEligibility.countryCities().test(with(40))).isTrue();
  }
}
