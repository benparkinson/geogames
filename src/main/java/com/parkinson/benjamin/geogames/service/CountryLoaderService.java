package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.PlaceName;
import java.util.Comparator;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * The country names and aliases for autocomplete. They come from the country shapes, which hold the
 * same names and aliases and are already parsed in memory, instead of from the 24 MB of full
 * geometry in {@code processed/countries}, which the app never reads. Antarctica has no shape, so
 * it is not in the list.
 */
@Service
public class CountryLoaderService {

  private final CountryShapeService shapes;

  public CountryLoaderService(CountryShapeService shapes) {
    this.shapes = shapes;
  }

  @Cacheable("countryNames")
  public List<PlaceName> loadCountryNames() {
    return shapes.loadShapes().stream()
        .map(
            shape ->
                new PlaceName(
                    shape.name(),
                    shape.additionalNames() == null ? List.of() : shape.additionalNames()))
        .sorted(Comparator.comparing(PlaceName::name))
        .toList();
  }
}
