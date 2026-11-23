package com.parkinson.benjamin.geogames.model;

import java.util.List;

/**
 * The data for one round of any river game type.
 *
 * @param countries the countries the river flows through, for the games that show them
 * @param focus the box the map opens on, or null for the games that show no countries
 */
public record RiverRound(GameType gameType, River river, List<Country> countries, Bounds focus)
    implements GameData {

  public RiverRound {
    countries = countries == null ? List.of() : List.copyOf(countries);
  }

  @Override
  public String subjectId() {
    return SubjectIds.river(river.id());
  }
}
