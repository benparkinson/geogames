package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.PlaceName;
import java.util.Comparator;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class LakeLoaderService {

  private final JsonResourceReader reader;

  public LakeLoaderService(JsonResourceReader reader) {
    this.reader = reader;
  }

  /** Reads the lakes generated offline by {@code LakeLoaderMain}. */
  @Cacheable("lakes")
  public List<Lake> loadLakes() {
    return reader.readAll("classpath*:/processed/lakes/*.json", Lake.class);
  }

  /** The names and aliases of every lake, sorted by name, for autocomplete and spell checking. */
  @Cacheable("lakeNames")
  public List<PlaceName> loadLakeNames() {
    return loadLakes().stream()
        .map(
            lake ->
                new PlaceName(
                    lake.name(),
                    lake.additionalNames() == null ? List.of() : lake.additionalNames()))
        .sorted(Comparator.comparing(PlaceName::name))
        .toList();
  }
}
