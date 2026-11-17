package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.CountryCities;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Reads the cities generated offline by {@code CityLoaderMain}. The single file is parsed once, on
 * first use, into records of primitives (about 4,000 cities, well under a megabyte), and every
 * method serves from that copy.
 */
@Service
public class CityService {

  private record Snapshot(List<CountryCities> all, Map<String, CountryCities> byCountry) {}

  private final JsonResourceReader reader;
  private Snapshot snapshot;

  public CityService(JsonResourceReader reader) {
    this.reader = reader;
  }

  /** Every country that has cities, sorted by name. */
  public List<CountryCities> loadAll() {
    return snapshot().all();
  }

  /** The cities of one country, or null if the file has none for it. */
  public CountryCities find(String country) {
    return snapshot().byCountry().get(country);
  }

  /** Synchronised, so two first requests cannot both read the file; later calls are cheap. */
  private synchronized Snapshot snapshot() {
    if (snapshot == null) {
      List<CountryCities> all =
          List.of(reader.read("processed/cities.json", CountryCities[].class));
      snapshot =
          new Snapshot(
              all,
              all.stream()
                  .collect(
                      Collectors.toUnmodifiableMap(CountryCities::country, Function.identity())));
    }
    return snapshot;
  }
}
