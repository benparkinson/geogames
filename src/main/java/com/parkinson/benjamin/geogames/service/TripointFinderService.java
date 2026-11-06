package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.dao.GameData;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TripointFinderService {

  private final Random random = new Random(System.currentTimeMillis());

  private final TripointLoaderService tripointLoaderService;
  private final CountryLoaderService countryLoaderService;

  @Autowired
  public TripointFinderService(
      TripointLoaderService tripointLoaderService, CountryLoaderService countryLoaderService) {
    this.tripointLoaderService = tripointLoaderService;
    this.countryLoaderService = countryLoaderService;
  }

  public List<GameData> findRandomTripoints(int howMany) throws IOException {
    List<TripointDefinition> definitions = tripointLoaderService.loadTripointDefinitions();
    if (definitions.size() < howMany) {
      throw new IllegalArgumentException(
          "Can't give you %d tripoints, I only know about %d!"
              .formatted(howMany, definitions.size()));
    }

    List<Integer> indices = new ArrayList<>(definitions.size());
    for (int i = 0; i < definitions.size(); i++) {
      indices.add(i);
    }
    Collections.shuffle(indices, random);

    List<GameData> tripoints = new ArrayList<>(howMany);
    for (int i = 0; i < howMany; i++) {
      TripointDefinition definition = definitions.get(indices.get(i));
      Set<Country> countries = new HashSet<>();
      for (String countryName : definition.countryNames()) {
        countries.add(countryLoaderService.loadCountryByName(countryName));
      }
      tripoints.add(new Tripoint(definition.coordinate(), countries));
    }
    return tripoints;
  }
}
