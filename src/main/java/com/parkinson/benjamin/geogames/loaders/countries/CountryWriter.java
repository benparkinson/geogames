package com.parkinson.benjamin.geogames.loaders.countries;

import static com.parkinson.benjamin.geogames.helper.CountryHelper.convertCountryNameToFileName;

import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class CountryWriter {

  private final ObjectMapper objectMapper =
      JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

  public void writeCountries(List<Country> countries, Path destinationFolder) throws IOException {

    for (Country country : countries) {
      String value = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(country);

      Files.write(
          Path.of(destinationFolder.toString(), convertCountryNameToFileName(country.name())),
          List.of(value));
    }
  }
}
