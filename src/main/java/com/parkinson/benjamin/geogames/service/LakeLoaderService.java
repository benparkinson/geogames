package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.PolygonCentroid;
import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.LocatedPlaceName;
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

  /** The names, aliases and positions of every lake, sorted by name, for autocomplete, spell checking
   * and distance clues. */
  @Cacheable("lakeNames")
  public List<LocatedPlaceName> loadLakeNames() {
    return loadLakes().stream()
        .map(
            lake -> {
              double[] position = PolygonCentroid.of(lake.geoData());
              return LocatedPlaceName.of(
                  lake.name(), lake.additionalNames(), position[0], position[1]);
            })
        .sorted(Comparator.comparing(LocatedPlaceName::name))
        .toList();
  }
}
