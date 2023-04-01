package com.parkinson.benjamin.geogames.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.model.geojson.CountryGeoData;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class CountryLoaderService {

  private final ObjectMapper objectMapper = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Cacheable("countripomes")
  public List<CountryGeoData> loadCountries() throws IOException {
    File coordinates = new ClassPathResource("data/countries.geo.json").getFile();

    CountryGeoData[] countries = objectMapper.readValue(coordinates, CountryGeoData[].class);
    return Arrays.asList(countries);
  }
}
