package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TripointFinderService implements RoundGenerator {

  private final TripointLoaderService tripointLoaderService;
  private final CountryLoaderService countryLoaderService;
  private final Random random;

  public TripointFinderService(
      TripointLoaderService tripointLoaderService,
      CountryLoaderService countryLoaderService,
      Random random) {
    this.tripointLoaderService = tripointLoaderService;
    this.countryLoaderService = countryLoaderService;
    this.random = random;
  }

  @Override
  public GameType gameType() {
    return GameType.TRIPOINT;
  }

  @Override
  public List<GameData> generate(int count, Set<String> excludedSubjects) {
    List<TripointDefinition> chosen =
        RandomPicker.pickDistinct(
            tripointLoaderService.loadTripointDefinitions().stream()
                .filter(d -> !excludedSubjects.contains(Tripoint.subjectId(d.countryNames())))
                .toList(),
            count,
            random,
            "tripoints");

    List<GameData> tripoints = new ArrayList<>(count);
    for (TripointDefinition definition : chosen) {
      Set<Country> countries = new HashSet<>();
      for (String countryName : definition.countryNames()) {
        countries.add(countryLoaderService.loadCountryByName(countryName));
      }
      tripoints.add(new Tripoint(definition.coordinate(), countries));
    }
    return tripoints;
  }
}
