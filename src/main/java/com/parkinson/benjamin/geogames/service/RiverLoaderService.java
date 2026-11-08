package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.PlaceName;
import com.parkinson.benjamin.geogames.model.River;
import java.util.Comparator;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class RiverLoaderService {

  private final JsonResourceReader reader;

  public RiverLoaderService(JsonResourceReader reader) {
    this.reader = reader;
  }

  /** Reads the rivers generated offline by {@code RiverLoaderMain}. */
  @Cacheable("rivers")
  public List<River> loadRivers() {
    return reader.readAll("classpath*:/processed/rivers/*.json", River.class);
  }

  /** The names and aliases of every river, sorted by name, for autocomplete and spell checking. */
  @Cacheable("riverNames")
  public List<PlaceName> loadRiverNames() {
    return loadRivers().stream()
        .map(
            river ->
                new PlaceName(
                    river.name(),
                    river.additionalNames() == null ? List.of() : river.additionalNames()))
        .sorted(Comparator.comparing(PlaceName::name))
        .toList();
  }
}
