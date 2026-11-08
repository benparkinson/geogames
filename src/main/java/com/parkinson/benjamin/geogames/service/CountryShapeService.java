package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryShape;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Reads the simplified country shapes generated offline by {@code CountryShapeMain}. Every shape is
 * parsed once, on first use, and every method serves from that single copy.
 */
@Service
public class CountryShapeService {

  private record Snapshot(List<CountryShape> all, Map<String, CountryShape> byName) {}

  private final JsonResourceReader reader;
  private Snapshot snapshot;

  public CountryShapeService(JsonResourceReader reader) {
    this.reader = reader;
  }

  public CountryShape loadShape(String name) {
    CountryShape shape = snapshot().byName().get(name);
    if (shape == null) {
      throw new UncheckedIOException(
          "No country shape for " + name, new IOException("unknown country: " + name));
    }
    return shape;
  }

  /** A country with its simplified outline: what the games draw, at a fraction of the full geometry's size. */
  @Cacheable("displayCountry")
  public Country loadDisplayCountry(String name) {
    CountryShape shape = loadShape(name);
    List<String> aliases = shape.additionalNames() == null ? List.of() : shape.additionalNames();
    return new Country(shape.name(), aliases, shape.outline());
  }

  public List<CountryShape> loadShapes() {
    return snapshot().all();
  }

  /** Synchronised, so two first requests cannot both read the files; later calls are cheap. */
  private synchronized Snapshot snapshot() {
    if (snapshot == null) {
      List<CountryShape> all =
          List.copyOf(
              reader.readAll("classpath*:/processed/country-shapes/*.json", CountryShape.class));
      Map<String, CountryShape> byName = new LinkedHashMap<>();
      all.forEach(shape -> byName.put(shape.name(), shape));
      snapshot = new Snapshot(all, Map.copyOf(byName));
    }
    return snapshot;
  }
}
