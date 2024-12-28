package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.helper.TripointHelper.getCoordinates;

import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.model.Coordinate;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TripointLoaderService extends FileLoaderService {

  @Cacheable("tripoints")
  public List<Tripoint> loadTripoints() throws IOException {
    Tripoint[] tripoints = loadFile("processed/tripoints.json", Tripoint[].class);

    return Arrays.asList(tripoints);
  }
}
