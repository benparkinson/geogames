package com.parkinson.benjamin.geogames.model;

import java.util.List;

/**
 * The data for one round of any country-shape game type.
 *
 * @param neighbours the country's mainland neighbours, for the games that show them
 */
public record CountryShapeRound(
    GameType gameType, CountryShape country, List<CountryOutline> neighbours) implements GameData {

  public CountryShapeRound {
    neighbours = neighbours == null ? List.of() : List.copyOf(neighbours);
  }

  /** Shared by all the country-shape games, so a mixed game never asks about France twice. */
  @Override
  public String subjectId() {
    return "country:" + country.name();
  }
}
