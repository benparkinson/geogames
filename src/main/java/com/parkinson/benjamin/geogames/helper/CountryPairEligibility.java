package com.parkinson.benjamin.geogames.helper;

import com.parkinson.benjamin.geogames.model.CountryShape;
import java.util.function.BiPredicate;

/**
 * Which pairs of countries each pair game can ask about. A pair is tested with the country that
 * comes first by name as the first argument.
 */
public final class CountryPairEligibility {

  /**
   * Under 50 km the shared border is a short stub that nobody can recognise (and the map has to
   * zoom in so far that the relief map at the end is a blur).
   */
  public static final double MIN_BORDER_KM = 50;

  /**
   * Merged Countries: the larger country may be at most this many times the area of the smaller.
   * Beyond that the smaller is a bump on the larger and the merged shape is just the larger one.
   */
  public static final double MERGED_MAX_AREA_RATIO = 8;

  private CountryPairEligibility() {}

  /**
   * Both games: each country is a mainland neighbour of the other, so that France and Brazil (who
   * meet only in French Guiana) are out and the map never has to frame an overseas part, and the
   * border is long enough to recognise.
   */
  public static BiPredicate<CountryShape, CountryShape> borders() {
    return (a, b) -> {
      CountryShape.Neighbour ab = neighbour(a, b);
      CountryShape.Neighbour ba = neighbour(b, a);
      return ab != null && ba != null && ab.mainland() && ba.mainland() && ab.km() >= MIN_BORDER_KM;
    };
  }

  /** Border Shapes: any bordering pair. */
  public static BiPredicate<CountryShape, CountryShape> borderShapes() {
    return borders();
  }

  /** Merged Countries: a bordering pair of similar size. */
  public static BiPredicate<CountryShape, CountryShape> mergedCountries() {
    return borders()
        .and(
            (a, b) -> {
              long smaller = Math.min(a.areaKm2(), b.areaKm2());
              long larger = Math.max(a.areaKm2(), b.areaKm2());
              return smaller > 0 && larger <= MERGED_MAX_AREA_RATIO * smaller;
            });
  }

  /** The entry for {@code other} in the country's neighbours, or null if they don't border. */
  public static CountryShape.Neighbour neighbour(CountryShape country, CountryShape other) {
    return country.neighbours().stream()
        .filter(n -> n.name().equals(other.name()))
        .findFirst()
        .orElse(null);
  }
}
