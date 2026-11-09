package com.parkinson.benjamin.geogames.model;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public record Tripoint(Coordinate coordinate, Set<Country> countries) implements GameData {

  @Override
  public GameType gameType() {
    return GameType.TRIPOINT;
  }

  @Override
  public String subjectId() {
    return subjectId(countries.stream().map(Country::name).toList());
  }

  /**
   * Seven sets of countries have two tripoints, so the subject id alone doesn't name the round: the
   * key adds the coordinate.
   */
  @Override
  public String roundKey() {
    return roundKey(countries.stream().map(Country::name).toList(), coordinate);
  }

  public static String roundKey(TripointDefinition definition) {
    return roundKey(definition.countryNames(), definition.coordinate());
  }

  private static String roundKey(Collection<String> countryNames, Coordinate coordinate) {
    return subjectId(countryNames)
        + "@"
        + Double.toString(coordinate.latitude())
        + ","
        + Double.toString(coordinate.longitude());
  }

  /** The id for a tripoint of these countries, whatever order they are in. */
  public static String subjectId(Collection<String> countryNames) {
    return "tripoint:" + countryNames.stream().sorted().collect(Collectors.joining("|"));
  }
}
