package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class RiverFinderService {

  private final Random random = new Random(System.currentTimeMillis());

  public GeoData findRiver(List<GeoData> rivers) {
    return rivers.get(random.nextInt(rivers.size()));
  }

}
