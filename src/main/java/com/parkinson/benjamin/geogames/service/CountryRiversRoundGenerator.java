package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.CountryRiversEligibility;
import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import com.parkinson.benjamin.geogames.model.CountryRiversRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.SubjectIds;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Makes the rounds of Country Rivers: the rivers inside a country. {@code generate} filters the
 * small index, so it reads no geometry until a country is picked.
 */
@Service
public class CountryRiversRoundGenerator extends SubjectRoundGenerator<CountryRiversIndexEntry> {

  private final CountryRiversService rivers;
  private final CountryShapeService shapes;

  public CountryRiversRoundGenerator(CountryRiversService rivers, CountryShapeService shapes) {
    super(GameType.COUNTRIES_BY_RIVERS, SubjectIds.COUNTRY, "countries");
    this.rivers = rivers;
    this.shapes = shapes;
  }

  @Override
  List<CountryRiversIndexEntry> candidates() {
    Set<String> withShape =
        shapes.loadShapes().stream().map(CountryShape::name).collect(Collectors.toSet());
    return rivers.index().stream()
        .filter(CountryRiversEligibility.countryRivers())
        .filter(entry -> withShape.contains(entry.name()))
        .toList();
  }

  @Override
  String id(CountryRiversIndexEntry entry) {
    return entry.name();
  }

  /** Doesn't check eligibility: the round was eligible when the game was created. */
  @Override
  Optional<CountryRiversIndexEntry> find(String name) {
    return Optional.ofNullable(rivers.find(name))
        .filter(entry -> shapes.findShape(name).isPresent());
  }

  /** Reads the country's file: the geometry is never held in memory between rounds. */
  @Override
  GameData toRound(CountryRiversIndexEntry entry) {
    CountryRivers file = rivers.load(entry.name());
    return new CountryRiversRound(
        gameType(),
        shapes.loadShape(entry.name()),
        file.major(),
        file.minor(),
        file.lakes(),
        file.longestRiver());
  }
}
