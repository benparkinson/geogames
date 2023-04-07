package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CountryLoaderService extends FileLoaderService {

  public CountryLoaderService() {
    super();
  }

  @Cacheable("countries")
  public List<GeoData> loadCountries() throws IOException {
    GeoData[] countries = loadFile("data/countries.geo.json", GeoData[].class);
    Map<String, List<String>> additionalNamesByCountryName =
        loadFile("data/additionalNames.json", Map.class);

    Arrays.stream(countries)
        .forEach(geoData ->
            geoData.getProperties().setAdditionalNames(
                additionalNamesByCountryName.getOrDefault(
                    geoData.getProperties().getName(),
                    Collections.emptyList())));

    return Arrays.asList(countries);
  }
}
