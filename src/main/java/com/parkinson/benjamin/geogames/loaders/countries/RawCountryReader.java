package com.parkinson.benjamin.geogames.loaders.countries;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads the raw Natural Earth export plus the alias file into {@link Country} objects, skipping the
 * features named in the exclusion file (non-country features such as bases and reefs).
 */
public class RawCountryReader {

  private final ObjectMapper objectMapper = GeoJson.lenientReader();

  /**
   * @throws IllegalArgumentException if an excluded name matches no feature in the source
   */
  @SuppressWarnings("unchecked")
  public List<Country> readCountries(
      Path countriesGeoJson, Path additionalNamesJson, Path excludedNamesJson) throws IOException {
    GeoData[] countryGeoData = objectMapper.readValue(countriesGeoJson.toFile(), GeoData[].class);
    Map<String, List<String>> additionalNamesByCountryName =
        objectMapper.readValue(additionalNamesJson.toFile(), Map.class);
    Set<String> excludedNames =
        new LinkedHashSet<>(
            Arrays.asList(objectMapper.readValue(excludedNamesJson.toFile(), String[].class)));

    Set<String> unmatched = new LinkedHashSet<>(excludedNames);
    for (GeoData geoData : countryGeoData) {
      unmatched.remove(geoData.properties().name());
    }
    if (!unmatched.isEmpty()) {
      throw new IllegalArgumentException(
          "Excluded names match no feature in " + countriesGeoJson + ": " + unmatched);
    }

    return Arrays.stream(countryGeoData)
        .filter(geoData -> !excludedNames.contains(geoData.properties().name()))
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
