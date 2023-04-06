package com.parkinson.benjamin.geogames.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.model.geojson.CountryGeoData;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class CountryLoaderService {

  private final ObjectMapper objectMapper = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Cacheable("countries")
  public List<CountryGeoData> loadCountries() throws IOException {
    InputStream coordinates = new ClassPathResource("data/countries.geo.json").getInputStream();

    CountryGeoData[] countries = objectMapper.readValue(coordinates, CountryGeoData[].class);

    Map<String, List<String>> additionalNamesByCountryName = lookupAdditionalNames();

    Arrays.stream(countries)
        .forEach(countryGeoData ->
            countryGeoData.getProperties().setAdditionalNames(
                additionalNamesByCountryName.getOrDefault(
                    countryGeoData.getProperties().getName(),
                    Collections.emptyList())));

    return Arrays.asList(countries);
  }

  public Map<String, List<String>> lookupAdditionalNames() throws IOException {
    InputStream coordinates = new ClassPathResource("data/additionalNames.json").getInputStream();

    return objectMapper.readValue(coordinates, Map.class);
  }
}
