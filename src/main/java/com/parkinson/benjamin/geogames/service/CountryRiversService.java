package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Reads the rivers and lakes of each country, generated offline by {@code CountryRiversMain}.
 *
 * <p>Only the small index (name, counts and length of each country) is kept in memory. A country's
 * geometry is the one large new data set, so {@link #load} reads that country's file on every call and
 * keeps nothing: a file is at most about 150 KB and a round is built once each time it is shown, so
 * parsing it is cheap, and a cache of the files would put several megabytes on the heap for good.
 * Don't add {@code @Cacheable} to it.
 */
@Service
public class CountryRiversService {

  private static final String FOLDER = "processed/country-rivers/";

  private record Snapshot(
      List<CountryRiversIndexEntry> all, Map<String, CountryRiversIndexEntry> byName) {}

  private final JsonResourceReader reader;
  private Snapshot snapshot;

  public CountryRiversService(JsonResourceReader reader) {
    this.reader = reader;
  }

  /** What is in each country's file, sorted by name. Read once. */
  public List<CountryRiversIndexEntry> index() {
    return snapshot().all();
  }

  /** The index entry of one country, or null if it has no rivers file. */
  public CountryRiversIndexEntry find(String country) {
    return snapshot().byName().get(country);
  }

  /**
   * Reads one country's file, by its exact path, every time.
   *
   * @throws UncheckedIOException if the country is not in the index
   */
  public CountryRivers load(String country) {
    CountryRiversIndexEntry entry = find(country);
    if (entry == null) {
      throw new UncheckedIOException(
          "No rivers for " + country, new IOException("unknown country: " + country));
    }
    return reader.read(FOLDER + entry.file(), CountryRivers.class);
  }

  /** Synchronised, so two first requests cannot both read the file; later calls are cheap. */
  private synchronized Snapshot snapshot() {
    if (snapshot == null) {
      List<CountryRiversIndexEntry> all =
          List.of(reader.read(FOLDER + "index.json", CountryRiversIndexEntry[].class));
      snapshot =
          new Snapshot(
              all,
              all.stream()
                  .collect(
                      Collectors.toUnmodifiableMap(
                          CountryRiversIndexEntry::name, Function.identity())));
    }
    return snapshot;
  }
}
