package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.CountryCentroid;
import com.parkinson.benjamin.geogames.model.CountryName;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * The country names and aliases for autocomplete. They come from the country shapes, which hold the
 * same names and aliases and are already parsed in memory, instead of from the 24 MB of full
 * geometry in {@code processed/countries}, which the app never reads. Antarctica has no shape, so
 * it is not in the list.
 */
@Service
public class CountryNameService {

  /**
   * Disputed territories with a shape that no game asks about or draws as a neighbour, so nobody
   * needs to guess them. (Palestine, Kosovo, Somaliland and Western Sahara take part in games.)
   */
  static final Set<String> NOT_IN_AUTOCOMPLETE = Set.of("Northern Cyprus");

  private final CountryShapeService shapes;

  public CountryNameService(CountryShapeService shapes) {
    this.shapes = shapes;
  }

  @Cacheable("countryNames")
  public List<CountryName> loadCountryNames() {
    return shapes.loadShapes().stream()
        .filter(shape -> !NOT_IN_AUTOCOMPLETE.contains(shape.name()))
        .map(
            shape -> {
              double[] position = CountryCentroid.of(shape.outline());
              return new CountryName(
                  shape.name(),
                  shape.additionalNames() == null ? List.of() : shape.additionalNames(),
                  position[0],
                  position[1]);
            })
        .sorted(Comparator.comparing(CountryName::name))
        .toList();
  }
}
