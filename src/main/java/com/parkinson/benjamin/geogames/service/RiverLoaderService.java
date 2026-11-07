package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

@Service
public class RiverLoaderService {

  /** What {@code additionalData.json} says about one river. */
  record RiverMetadata(List<String> names, String continent) {}

  private final JsonResourceReader reader;

  public RiverLoaderService(JsonResourceReader reader) {
    this.reader = reader;
  }

  @Cacheable("rivers")
  public List<River> loadRivers() {
    Map<String, RiverMetadata> metadataByRiverName =
        reader.read("data/rivers/additionalData.json", new TypeReference<>() {});
    List<River> rivers = new ArrayList<>();

    for (GeoData geoData : reader.readAll("classpath*:/data/rivers/*.geo.json", GeoData.class)) {
      String name = geoData.properties().name();
      RiverMetadata metadata = metadataByRiverName.get(name);
      if (metadata == null) {
        throw new IllegalStateException(
            "River %s has no entry in data/rivers/additionalData.json".formatted(name));
      }
      List<String> additionalNames = metadata.names() == null ? List.of() : metadata.names();
      rivers.add(new River(name, additionalNames, metadata.continent(), geoData));
    }

    return rivers;
  }
}
