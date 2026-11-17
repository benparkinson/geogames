package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.CountryCitiesEligibility;
import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.CountryCities;
import com.parkinson.benjamin.geogames.model.CountryCitiesRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Makes the rounds of Country Cities: a country's cities as dots. */
@Service
public class CountryCitiesRoundGenerator implements RoundGenerator {

  private static final String PREFIX = "country:";

  private final CityService cities;
  private final CountryShapeService shapes;

  public CountryCitiesRoundGenerator(CityService cities, CountryShapeService shapes) {
    this.cities = cities;
    this.shapes = shapes;
  }

  @Override
  public GameType gameType() {
    return GameType.COUNTRIES_BY_CITIES;
  }

  @Override
  public List<GameData> generate(int count, Set<String> excludedSubjects, Random random) {
    Set<String> withShape =
        shapes.loadShapes().stream().map(CountryShape::name).collect(Collectors.toSet());
    List<CountryCities> candidates =
        cities.loadAll().stream()
            .filter(CountryCitiesEligibility.countryCities())
            .filter(c -> withShape.contains(c.country()))
            .filter(c -> !excludedSubjects.contains(PREFIX + c.country()))
            .toList();
    return RandomPicker.pickDistinct(candidates, count, random, "countries").stream()
        .<GameData>map(this::toRound)
        .toList();
  }

  /** Doesn't check eligibility: the round was eligible when the game was created. */
  @Override
  public GameData rebuild(String roundKey) {
    if (!roundKey.startsWith(PREFIX)) {
      throw new RoundDataMissingException(gameType(), roundKey);
    }
    String name = roundKey.substring(PREFIX.length());
    CountryCities found = cities.find(name);
    if (found == null || shapes.loadShapes().stream().noneMatch(s -> s.name().equals(name))) {
      throw new RoundDataMissingException(gameType(), roundKey);
    }
    return toRound(found);
  }

  private CountryCitiesRound toRound(CountryCities found) {
    return new CountryCitiesRound(gameType(), shapes.loadShape(found.country()), found.cities());
  }
}
