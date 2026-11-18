package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TripointFinderService implements RoundGenerator {

  private final TripointLoaderService tripointLoaderService;
  private final CountryShapeService countryShapeService;

  public TripointFinderService(
      TripointLoaderService tripointLoaderService, CountryShapeService countryShapeService) {
    this.tripointLoaderService = tripointLoaderService;
    this.countryShapeService = countryShapeService;
  }

  @Override
  public GameType gameType() {
    return GameType.TRIPOINT;
  }

  @Override
  public List<GameData> generate(int count, Set<String> excludedSubjects, Random random) {
    // Seven country triples have two tripoints: pick distinct subjects, not distinct definitions,
    // so a game never asks for the same three countries twice.
    Map<String, List<TripointDefinition>> bySubject = new LinkedHashMap<>();
    for (TripointDefinition definition : tripointLoaderService.loadTripointDefinitions()) {
      String subject = Tripoint.subjectId(definition.countryNames());
      if (!excludedSubjects.contains(subject)) {
        bySubject.computeIfAbsent(subject, k -> new ArrayList<>()).add(definition);
      }
    }
    List<TripointDefinition> chosen = new ArrayList<>(count);
    for (List<TripointDefinition> group :
        RandomPicker.pickDistinct(
            new ArrayList<>(bySubject.values()), count, random, "tripoints")) {
      chosen.add(group.get(random.nextInt(group.size())));
    }

    List<GameData> tripoints = new ArrayList<>(count);
    for (TripointDefinition definition : chosen) {
      tripoints.add(toRound(definition));
    }
    return tripoints;
  }

  @Override
  public GameData rebuild(String roundKey) {
    return tripointLoaderService.loadTripointDefinitions().stream()
        .filter(d -> Tripoint.roundKey(d).equals(roundKey))
        .findFirst()
        .map(this::toRound)
        .orElseThrow(() -> new RoundDataMissingException(GameType.TRIPOINT, roundKey));
  }

  /** In the definition's order, so a rebuilt round is the same JSON as the generated one. */
  private Tripoint toRound(TripointDefinition definition) {
    Set<Country> countries = new LinkedHashSet<>();
    for (String countryName : definition.countryNames()) {
      countries.add(countryShapeService.loadDisplayCountry(countryName));
    }
    return new Tripoint(definition.coordinate(), countries);
  }
}
