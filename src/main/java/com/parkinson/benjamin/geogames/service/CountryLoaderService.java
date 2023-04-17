package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Country;
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
  public List<Country> loadCountries() throws IOException {
    GeoData[] countryGeoData = loadFile("data/countries/countries.geo.json", GeoData[].class);
    Map<String, List<String>> additionalNamesByCountryName =
        loadFile("data/countries/additionalNames.json", Map.class);

    return Arrays.stream(countryGeoData)
        .map(geoData -> {
          List<String> additionalNames = additionalNamesByCountryName.getOrDefault(
              geoData.properties().name(), Collections.emptyList());
          return new Country(geoData.properties().name(), additionalNames, geoData);
        }).toList();
  }
}
