package com.parkinson.benjamin.geogames.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinson.benjamin.geogames.model.Country;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class CountryLoaderService {

  @Cacheable("countries")
  public List<Country> loadCountries() throws IOException {
    File coordinates = new ClassPathResource("data/countries.json").getFile();
    ObjectMapper objectMapper = new ObjectMapper();
    Country[] countries = objectMapper.readValue(coordinates, Country[].class);
    return Arrays.asList(countries);
  }
}
