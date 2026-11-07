package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.CountryName;
import com.parkinson.benjamin.geogames.service.CountryLoaderService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class CountryHelperTest {

  @ParameterizedTest
  @CsvSource({
    "France, France.json",
    "United States of America, UnitedStatesofAmerica.json",
    "St. Lucia, StLucia.json",
    "Curaçao, Curaçao.json"
  })
  void convertsCountryNameToFileName(String name, String expected) {
    assertThat(CountryHelper.convertCountryNameToFileName(name)).isEqualTo(expected);
  }

  @Test
  void everyCountryNameMapsToAFileOnTheClasspath() throws IOException {
    CountryLoaderService service =
        new CountryLoaderService(new PathMatchingResourcePatternResolver());

    for (CountryName country : service.loadCountryNames()) {
      String path =
          "processed/countries/" + CountryHelper.convertCountryNameToFileName(country.name());
      assertThat(new ClassPathResource(path).exists()).as(path).isTrue();
    }
  }
}
