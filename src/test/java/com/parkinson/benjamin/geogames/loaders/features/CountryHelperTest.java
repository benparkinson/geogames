package com.parkinson.benjamin.geogames.loaders.features;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.PlaceName;
import com.parkinson.benjamin.geogames.service.JsonResourceReader;
import java.io.IOException;
import java.util.List;
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
    "Curaçao, Curacao.json",
    "Åland, Aland.json",
    "Türkiye, Turkiye.json",
    "'Côte d''Ivoire', CotedIvoire.json",
    "Timor-Leste, Timor-Leste.json"
  })
  void convertsCountryNameToFileName(String name, String expected) {
    assertThat(CountryHelper.convertCountryNameToFileName(name)).isEqualTo(expected);
  }

  @Test
  void everyCountryNameMapsToAFileOnTheClasspath() throws IOException {
    JsonResourceReader reader = new JsonResourceReader(new PathMatchingResourcePatternResolver());

    for (PlaceName country : rawCountryNames(reader)) {
      String path =
          "processed/countries/" + CountryHelper.convertCountryNameToFileName(country.name());
      assertThat(new ClassPathResource(path).exists()).as(path).isTrue();
    }
  }

  /** The names in the raw {@code processed/countries} files, which the app itself never reads. */
  private static List<PlaceName> rawCountryNames(JsonResourceReader reader) {
    return reader.readAll("classpath*:/processed/countries/*.json", PlaceName.class);
  }
}
