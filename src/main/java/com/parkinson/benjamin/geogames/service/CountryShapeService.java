package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryShape;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.function.SingletonSupplier;

/**
 * Reads the simplified country shapes generated offline by {@code CountryShapeMain}. Every shape is
 * parsed once, on first use, and every method serves from that single copy.
 */
@Service
public class CountryShapeService {

  private record Snapshot(List<CountryShape> all, Map<String, CountryShape> byName) {}

  private final JsonResourceReader reader;
  private final SingletonSupplier<Snapshot> snapshot = SingletonSupplier.of(this::load);

  public CountryShapeService(JsonResourceReader reader) {
    this.reader = reader;
  }

  public CountryShape loadShape(String name) {
    CountryShape shape = snapshot.obtain().byName().get(name);
    if (shape == null) {
      throw new UncheckedIOException(
          "No country shape for " + name, new IOException("unknown country: " + name));
    }
    return shape;
  }

  /** The shape of the country with this name, or empty if there is none. */
  public Optional<CountryShape> findShape(String name) {
    return Optional.ofNullable(snapshot.obtain().byName().get(name));
  }

  /**
   * A country with its simplified outline: what the games draw, at a fraction of the full
   * geometry's size.
   */
  @Cacheable("displayCountry")
  public Country loadDisplayCountry(String name) {
    CountryShape shape = loadShape(name);
    List<String> aliases = shape.additionalNames() == null ? List.of() : shape.additionalNames();
    return new Country(shape.name(), aliases, shape.outline());
  }

  public List<CountryShape> loadShapes() {
    return snapshot.obtain().all();
  }

  private Snapshot load() {
    List<CountryShape> all =
        List.copyOf(
            reader.readAll("classpath*:/processed/country-shapes/*.json", CountryShape.class));
    Map<String, CountryShape> byName = new LinkedHashMap<>();
    all.forEach(shape -> byName.put(shape.name(), shape));
    return new Snapshot(all, Map.copyOf(byName));
  }
}
