package com.parkinson.benjamin.geogames.model;

import java.util.List;

/**
 * The data for one round of Country Cities.
 *
 * @param country the country's shape, from the shapes the app already holds
 * @param cities its largest cities and its capital, largest first
 */
public record CountryCitiesRound(GameType gameType, CountryShape country, List<City> cities)
    implements GameData {

  public CountryCitiesRound {
    cities = List.copyOf(cities);
  }

  /** Shared with the country-shape games, so a mixed game never asks about one country twice. */
  @Override
  public String subjectId() {
    return "country:" + country.name();
  }
}
