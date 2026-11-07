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

  /** The id for a tripoint of these countries, whatever order they are in. */
  public static String subjectId(Collection<String> countryNames) {
    return "tripoint:" + countryNames.stream().sorted().collect(Collectors.joining("|"));
  }
}
