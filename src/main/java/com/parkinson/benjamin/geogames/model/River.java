package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;

public record River(String name, List<String> additionalNames, String continent, GeoData geoData)
    implements GameData {

  @Override
  public GameType gameType() {
    return GameType.RIVERS_BY_SHAPE;
  }
}
