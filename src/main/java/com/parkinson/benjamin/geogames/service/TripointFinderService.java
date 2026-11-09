package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.TripointDefinition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TripointFinderService implements RoundGenerator {

  private final TripointLoaderService tripointLoaderService;
  private final CountryShapeService countryShapeService;
  private final Random random;

  public TripointFinderService(
      TripointLoaderService tripointLoaderService,
      CountryShapeService countryShapeService,
      Random random) {
    this.tripointLoaderService = tripointLoaderService;
    this.countryShapeService = countryShapeService;
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
