package com.parkinson.benjamin.geogames.loaders.countries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.parkinson.benjamin.geogames.helper.CountryHelper.convertCountryNameToFileName;

public class CountryWriter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public void writeCountries(List<Country> countries, Path destinationFolder) throws IOException {

    for (Country country : countries) {
      String value = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(country);

      Files.write(
          Path.of(destinationFolder.toString(), convertCountryNameToFileName(country.name())),
          List.of(value));
    }

  }

}
