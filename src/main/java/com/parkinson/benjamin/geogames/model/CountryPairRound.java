package com.parkinson.benjamin.geogames.model;

import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import java.util.List;

/**
 * The data for one round of a pair game: two neighbouring countries and the border between them.
 * It holds references to the cached country shapes, so a round adds almost nothing to the heap.
 *
 * @param countries the two shapes, sorted by name
 * @param sharedBorder the lines they share, a MultiLineString
 * @param borderKm the length of the shared border
 * @param neighbours for Merged Countries, the mainland neighbours of either country except the pair
 *     itself, each once; empty for Border Shapes
 * @param focus the union of the two countries' focus boxes
 * @param borderBounds the bounding box of the shared border
 */
public record CountryPairRound(
    GameType gameType,
    List<CountryShape> countries,
    GeoData sharedBorder,
    long borderKm,
    List<CountryOutline> neighbours,
    Bounds focus,
    Bounds borderBounds)
    implements GameData {

  static final String PREFIX = "pair:";

  public CountryPairRound {
    countries = List.copyOf(countries);
    neighbours = neighbours == null ? List.of() : List.copyOf(neighbours);
  }

  /** Shared by both pair games, so a mixed game never asks about one pair twice. */
  @Override
  public String subjectId() {
    return subjectId(countries.get(0).name(), countries.get(1).name());
  }

  /** The names in either order give the same id. */
  public static String subjectId(String first, String second) {
    return first.compareTo(second) <= 0
        ? PREFIX + first + "|" + second
        : PREFIX + second + "|" + first;
  }
}
