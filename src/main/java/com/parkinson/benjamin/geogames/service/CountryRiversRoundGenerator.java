package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.helper.CountryRiversEligibility;
import com.parkinson.benjamin.geogames.helper.RandomPicker;
import com.parkinson.benjamin.geogames.model.CountryRivers;
import com.parkinson.benjamin.geogames.model.CountryRiversIndexEntry;
import com.parkinson.benjamin.geogames.model.CountryRiversRound;
import com.parkinson.benjamin.geogames.model.CountryShape;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Makes the rounds of Country Rivers: the rivers inside a country. {@code generate} filters the
 * small index, so it reads no geometry until a country is picked.
 */
@Service
public class CountryRiversRoundGenerator implements RoundGenerator {

  private static final String PREFIX = "country:";

  private final CountryRiversService rivers;
  private final CountryShapeService shapes;

  public CountryRiversRoundGenerator(CountryRiversService rivers, CountryShapeService shapes) {
    this.rivers = rivers;
    this.shapes = shapes;
  }

  @Override
  public GameType gameType() {
    return GameType.COUNTRIES_BY_RIVERS;
  }

  @Override
  public List<GameData> generate(int count, Set<String> excludedSubjects, Random random) {
    Set<String> withShape =
        shapes.loadShapes().stream().map(CountryShape::name).collect(Collectors.toSet());
    List<CountryRiversIndexEntry> candidates =
        rivers.index().stream()
            .filter(CountryRiversEligibility.countryRivers())
            .filter(entry -> withShape.contains(entry.name()))
            .filter(entry -> !excludedSubjects.contains(PREFIX + entry.name()))
            .toList();
    return RandomPicker.pickDistinct(candidates, count, random, "countries").stream()
        .<GameData>map(entry -> toRound(entry.name()))
        .toList();
  }

  /** Doesn't check eligibility: the round was eligible when the game was created. */
  @Override
  public GameData rebuild(String roundKey) {
    if (!roundKey.startsWith(PREFIX)) {
      throw new RoundDataMissingException(gameType(), roundKey);
    }
    String name = roundKey.substring(PREFIX.length());
    if (rivers.find(name) == null
        || shapes.loadShapes().stream().noneMatch(s -> s.name().equals(name))) {
      throw new RoundDataMissingException(gameType(), roundKey);
    }
    return toRound(name);
  }

  /** Reads the country's file: the geometry is never held in memory between rounds. */
  private CountryRiversRound toRound(String name) {
    CountryRivers file = rivers.load(name);
    return new CountryRiversRound(
        gameType(), shapes.loadShape(name), file.major(), file.minor(), file.lakes());
  }
}
