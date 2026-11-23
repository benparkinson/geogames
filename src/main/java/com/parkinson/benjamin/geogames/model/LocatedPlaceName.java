package com.parkinson.benjamin.geogames.model;

import java.util.List;

/**
 * A country, river or lake for autocomplete and distance clues.
 *
 * @param lat latitude of the place's position. A country or lake: the area-weighted centroid of the
 *     outer ring of its largest polygon part (so France is in Europe, not pulled towards French
 *     Guiana). A river: its vertex nearest to its length-weighted centroid.
 * @param lon longitude of the same position
 */
public record LocatedPlaceName(String name, List<String> additionalNames, double lat, double lon) {

  /** Like the constructor, but a null list of aliases becomes empty. */
  public static LocatedPlaceName of(
      String name, List<String> additionalNames, double lat, double lon) {
    return new LocatedPlaceName(
        name, additionalNames == null ? List.of() : additionalNames, lat, lon);
  }
}
