package com.parkinson.benjamin.geogames.loaders.countries;

import static com.parkinson.benjamin.geogames.helper.CountryHelper.convertCountryNameToFileName;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CountryWriter {

  public void writeCountries(List<Country> countries, Path destinationFolder) throws IOException {

    for (Country country : countries) {
      String value = GeoJson.prettyWriter().writeValueAsString(country);

      Files.write(
          Path.of(destinationFolder.toString(), convertCountryNameToFileName(country.name())),
          List.of(value));
    }
  }
}
