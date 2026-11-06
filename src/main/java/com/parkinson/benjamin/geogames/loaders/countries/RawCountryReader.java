package com.parkinson.benjamin.geogames.loaders.countries;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Reads the raw Natural Earth export plus the alias file into {@link Country} objects. */
public class RawCountryReader {

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @SuppressWarnings("unchecked")
  public List<Country> readCountries(Path countriesGeoJson, Path additionalNamesJson)
      throws IOException {
    GeoData[] countryGeoData = objectMapper.readValue(countriesGeoJson.toFile(), GeoData[].class);
    Map<String, List<String>> additionalNamesByCountryName =
        objectMapper.readValue(additionalNamesJson.toFile(), Map.class);

    return Arrays.stream(countryGeoData)
        .map(
            geoData -> {
              List<String> additionalNames =
                  additionalNamesByCountryName.getOrDefault(
                      geoData.properties().name(), Collections.emptyList());
              return new Country(geoData.properties().name(), additionalNames, geoData);
            })
        .toList();
  }
}
