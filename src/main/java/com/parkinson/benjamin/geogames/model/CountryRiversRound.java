package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;

/**
 * The data for one round of Waterways. The rivers and lakes are read from the country's file
 * when the round is built and are not cached.
 *
 * @param country the country's shape, from the shapes the app already holds
 * @param major MultiLineString: its most important rivers
 * @param minor MultiLineString: its other rivers
 * @param lakes MultiPolygon: its lakes, with no polygons when it has none
 * @param longestRiver the name of its longest named river, or null when it has none
 */
public record CountryRiversRound(
    GameType gameType,
    CountryShape country,
    GeoData major,
    GeoData minor,
    GeoData lakes,
    String longestRiver)
    implements GameData {

  /** Shared with the country-shape games, so a mixed game never asks about one country twice. */
  @Override
  public String subjectId() {
    return SubjectIds.country(country.name());
  }
}
