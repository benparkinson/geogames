package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.PlaceName;
import java.util.Comparator;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CountryLoaderService {

  private final JsonResourceReader reader;

  public CountryLoaderService(JsonResourceReader reader) {
    this.reader = reader;
  }

  @Cacheable("countryNames")
  public List<PlaceName> loadCountryNames() {
    return reader.readAll("classpath*:/processed/countries/*.json", PlaceName.class).stream()
        .map(
            country ->
                new PlaceName(
                    country.name(),
                    country.additionalNames() == null ? List.of() : country.additionalNames()))
        .sorted(Comparator.comparing(PlaceName::name))
        .toList();
  }
}
