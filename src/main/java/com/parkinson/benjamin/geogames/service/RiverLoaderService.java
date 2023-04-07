package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
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
  public List<GeoData> loadRivers() throws IOException {
    Resource[] resources = applicationContext.getResources("classpath*:/data/rivers/*.geo.json");

    List<GeoData> rivers = new ArrayList<>();

    for (Resource resource : resources) {
      rivers.add(readFile(resource.getInputStream(), GeoData.class));
    }

    return rivers;
  }
}
