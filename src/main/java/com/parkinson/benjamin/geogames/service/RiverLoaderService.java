package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class RiverLoaderService extends FileLoaderService {

  public RiverLoaderService() {
    super();
  }

  @Cacheable("rivers")
  public List<GeoData> loadRivers() throws IOException {
    File riversDir = new ClassPathResource("data/rivers").getFile();
    File[] files = riversDir.listFiles();

    List<GeoData> rivers = new ArrayList<>();

    for (File file : files) {
      rivers.add(readFile(new FileInputStream(file), GeoData.class));
    }

    return rivers;
  }
}
