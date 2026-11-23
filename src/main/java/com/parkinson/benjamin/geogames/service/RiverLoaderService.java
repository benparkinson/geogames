package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.RiverPosition;
import com.parkinson.benjamin.geogames.model.LocatedPlaceName;
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

  /** The names, aliases and positions of every river, sorted by name, for autocomplete, spell checking
   * and distance clues. */
  @Cacheable("riverNames")
  public List<LocatedPlaceName> loadRiverNames() {
    return loadRivers().stream()
        .map(
            river -> {
              double[] position = RiverPosition.of(river.geoData());
              return LocatedPlaceName.of(
                  river.name(), river.additionalNames(), position[0], position[1]);
            })
        .sorted(Comparator.comparing(LocatedPlaceName::name))
        .toList();
  }
}
