package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.Bounds;
import java.util.ArrayList;
import java.util.List;

/**
 * The box River Countries opens its map on: the river and every country's main outline, except
 * that a huge country is cut back so it doesn't shrink the others to dots.
 */
public final class RiverFocus {

  /** A country this many times the area of the rest of the round is cut back (Russia, the USA). */
  static final double BIG_RATIO = 5;

  /** How far past the rest of the round a cut-back country still shows, as a share of its size. */
  static final double MARGIN = 0.25;

  private RiverFocus() {}

  /** The river's box and each country's focus box in; the box to show out. */
  public static Bounds of(Bounds river, List<Bounds> countryFocuses) {
    Bounds result = river;
    for (int i = 0; i < countryFocuses.size(); i++) {
      Bounds focus = countryFocuses.get(i);
      List<Bounds> rest = new ArrayList<>(countryFocuses);
      rest.remove(i);
      Bounds others = river;
      for (Bounds other : rest) {
        others = others.union(other);
      }
      boolean big = focus.area() > BIG_RATIO * others.area();
      result = result.union(big ? focus.intersect(others.expand(MARGIN)) : focus);
    }
    return result;
  }
}
