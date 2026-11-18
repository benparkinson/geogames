package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.CountryCitiesEligibility;
import com.parkinson.benjamin.geogames.model.CountryCities;
import com.parkinson.benjamin.geogames.model.CountryCitiesRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.SubjectIds;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Makes the rounds of Country Cities: a country's cities as dots. */
@Service
public class CountryCitiesRoundGenerator extends SubjectRoundGenerator<CountryCities> {

  private final CityService cities;
  private final CountryShapeService shapes;

  public CountryCitiesRoundGenerator(CityService cities, CountryShapeService shapes) {
    super(GameType.COUNTRIES_BY_CITIES, SubjectIds.COUNTRY, "countries");
    this.cities = cities;
    this.shapes = shapes;
  }

  @Override
  List<CountryCities> candidates() {
    Set<String> withShape =
        shapes.loadShapes().stream().map(CountryShape::name).collect(Collectors.toSet());
    return cities.loadAll().stream()
        .filter(CountryCitiesEligibility.countryCities())
        .filter(c -> withShape.contains(c.country()))
        .toList();
  }

  @Override
  String id(CountryCities found) {
    return found.country();
  }

  /** Doesn't check eligibility: the round was eligible when the game was created. */
  @Override
  Optional<CountryCities> find(String name) {
    return Optional.ofNullable(cities.find(name))
        .filter(found -> shapes.findShape(name).isPresent());
  }

  @Override
  GameData toRound(CountryCities found) {
    return new CountryCitiesRound(gameType(), shapes.loadShape(found.country()), found.cities());
  }
}
