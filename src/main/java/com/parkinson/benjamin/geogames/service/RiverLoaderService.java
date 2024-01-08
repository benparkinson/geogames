package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class RiverLoaderService extends FileLoaderService {

  private final ApplicationContext applicationContext;

  @Autowired
  public RiverLoaderService(ApplicationContext applicationContext) {
    super();
    this.applicationContext = applicationContext;
  }

  @Cacheable("rivers")
  public List<River> loadRivers() throws IOException {
    Resource[] resources = applicationContext.getResources("classpath*:/data/rivers/*.geo.json");
    Map<String, Map> additionalDataByRiverName =
        loadFile("data/rivers/additionalData.json", Map.class);
    List<River> rivers = new ArrayList<>();

    for (Resource resource : resources) {
      GeoData geoData = readFile(resource.getInputStream(), GeoData.class);
      String name = geoData.properties().name();
      Map additionalData = additionalDataByRiverName.get(name);
      rivers.add(
          new River(
              name,
              (List<String>) additionalData.get("names"),
              (String) additionalData.get("continent"),
              geoData));
    }

    return rivers;
  }
}
