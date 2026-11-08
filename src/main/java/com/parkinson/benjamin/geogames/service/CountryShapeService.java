package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.helper.CountryHelper.convertCountryNameToFileName;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryShape;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Reads the simplified country shapes generated offline by {@code CountryShapeMain}. */
@Service
public class CountryShapeService {

  private final JsonResourceReader reader;

  public CountryShapeService(JsonResourceReader reader) {
    this.reader = reader;
  }

  @Cacheable("countryShape")
  public CountryShape loadShape(String name) {
    return reader.read(
        "processed/country-shapes/" + convertCountryNameToFileName(name), CountryShape.class);
  }

  /** A country with its simplified outline: what the games draw, at a fraction of the full geometry's size. */
  @Cacheable("displayCountry")
  public Country loadDisplayCountry(String name) {
    CountryShape shape = loadShape(name);
    List<String> aliases = shape.additionalNames() == null ? List.of() : shape.additionalNames();
    return new Country(shape.name(), aliases, shape.outline());
  }

  @Cacheable("countryShapes")
  public List<CountryShape> loadShapes() {
    return reader.readAll("classpath*:/processed/country-shapes/*.json", CountryShape.class);
  }
}
