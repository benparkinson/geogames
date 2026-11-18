package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.CountryCities;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.function.SingletonSupplier;

/**
 * Reads the cities generated offline by {@code CityLoaderMain}. The single file is parsed once, on
 * first use, into records of primitives (about 4,000 cities, well under a megabyte), and every
 * method serves from that copy.
 */
@Service
public class CityService {

  private record Snapshot(List<CountryCities> all, Map<String, CountryCities> byCountry) {}

  private final JsonResourceReader reader;
  private final SingletonSupplier<Snapshot> snapshot = SingletonSupplier.of(this::load);

  public CityService(JsonResourceReader reader) {
    this.reader = reader;
  }

  /** Every country that has cities, sorted by name. */
  public List<CountryCities> loadAll() {
    return snapshot.obtain().all();
  }

  /** The cities of one country, or null if the file has none for it. */
  public CountryCities find(String country) {
    return snapshot.obtain().byCountry().get(country);
  }

  private Snapshot load() {
    List<CountryCities> all = List.of(reader.read("processed/cities.json", CountryCities[].class));
    return new Snapshot(
        all,
        all.stream()
            .collect(Collectors.toUnmodifiableMap(CountryCities::country, Function.identity())));
  }
}
