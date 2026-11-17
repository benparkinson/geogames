package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.loaders.features.CountryLocator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Cuts a river's line into the stretches that lie in each country.
 *
 * <p>Natural Earth's rivers and the country borders are drawn separately, so a river that follows a
 * border falls on one side of it or the other. A segment therefore belongs to every country that its
 * midpoint, or any of four points {@link #NUDGE_DEGREES} north, south, east and west of the midpoint,
 * is in. A river along a border so goes to both countries, which is what the game wants, and one that
 * only grazes a corner doesn't.
 */
public class RiverClipper {

  /** About 3 km. */
  static final double NUDGE_DEGREES = 0.03;

  /**
   * A stretch of one river inside one country.
   *
   * @param scalerank Natural Earth's importance of the river: lower is more important
   * @param rivernum Natural Earth's number for the river, shared by all its lines
   * @param points {lon, lat}, at least two
   */
  public record Run(int scalerank, int rivernum, List<double[]> points) {}

  private final CountryLocator locator;

  public RiverClipper(CountryLocator locator) {
    this.locator = locator;
  }

  /**
   * The runs of this line in each country, keyed by country name (sorted). Consecutive segments in
   * the same country form a run, so a line that leaves a country and comes back gives it two.
   */
  public Map<String, List<Run>> clip(int scalerank, int rivernum, List<double[]> line) {
    Map<String, List<Run>> runs = new TreeMap<>();
    Map<String, List<double[]>> open = new LinkedHashMap<>();
    for (int i = 1; i < line.size(); i++) {
      double[] a = line.get(i - 1);
      double[] b = line.get(i);
      Set<String> countries = countriesOf(a, b);

      List<String> finished = new ArrayList<>();
      for (String country : open.keySet()) {
        if (!countries.contains(country)) {
          finished.add(country);
        }
      }
      for (String country : finished) {
        close(runs, open.remove(country), country, scalerank, rivernum);
      }
      for (String country : countries) {
        open.computeIfAbsent(country, key -> new ArrayList<>(List.of(a))).add(b);
      }
    }
    for (Map.Entry<String, List<double[]>> entry : open.entrySet()) {
      close(runs, entry.getValue(), entry.getKey(), scalerank, rivernum);
    }
    return runs;
  }

  private static void close(
      Map<String, List<Run>> runs,
      List<double[]> points,
      String country,
      int scalerank,
      int rivernum) {
    runs.computeIfAbsent(country, key -> new ArrayList<>())
        .add(new Run(scalerank, rivernum, points));
  }

  /** The countries the segment belongs to, sorted. */
  Set<String> countriesOf(double[] a, double[] b) {
    double lon = (a[0] + b[0]) / 2;
    double lat = (a[1] + b[1]) / 2;
    double[][] samples = {
      {lon, lat},
      {lon, lat + NUDGE_DEGREES},
      {lon, lat - NUDGE_DEGREES},
      {lon + NUDGE_DEGREES, lat},
      {lon - NUDGE_DEGREES, lat}
    };
    Set<String> countries = new TreeSet<>();
    for (double[] sample : samples) {
      String country = locator.countryAt(sample);
      if (country != null) {
        countries.add(country);
      }
    }
    return countries;
  }
}
