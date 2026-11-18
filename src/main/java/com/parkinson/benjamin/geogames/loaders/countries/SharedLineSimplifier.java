package com.parkinson.benjamin.geogames.loaders.countries;

import static com.parkinson.benjamin.geogames.loaders.countries.CountryRings.vertexKey;

import com.parkinson.benjamin.geogames.loaders.features.DouglasPeucker;
import com.parkinson.benjamin.geogames.loaders.features.GeoPoints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simplifies a line in a canonical direction, so the two countries that share it get identical
 * points and no gap opens between them.
 */
final class SharedLineSimplifier {

  private SharedLineSimplifier() {}

  /** Simplifies in the direction where the first vertex key is not greater than the last. */
  static List<double[]> simplifyOpen(List<double[]> points, double tolerance) {
    boolean reverse =
        vertexKey(points.get(0)).compareTo(vertexKey(points.get(points.size() - 1))) > 0;
    List<double[]> input = new ArrayList<>(points);
    if (reverse) {
      Collections.reverse(input);
    }
    List<double[]> result = DouglasPeucker.simplify(input, tolerance);
    if (reverse) {
      Collections.reverse(result);
    }
    return GeoPoints.roundLine(result);
  }

  /**
   * A closed run has no ends to anchor on, so it starts at its smallest vertex key, runs in the
   * direction whose second key is the smaller, and is cut at the vertex furthest from the start.
   * Whichever country calls this, the same ring gives the same points.
   */
  static List<double[]> simplifyClosed(List<double[]> closed, double tolerance) {
    int m = closed.size() - 1;
    int smallest = 0;
    for (int i = 1; i < m; i++) {
      if (vertexKey(closed.get(i)).compareTo(vertexKey(closed.get(smallest))) < 0) {
        smallest = i;
      }
    }
    List<double[]> ring = new ArrayList<>();
    for (int i = 0; i <= m; i++) {
      ring.add(closed.get((smallest + i) % m));
    }
    boolean reversed = vertexKey(ring.get(1)).compareTo(vertexKey(ring.get(m - 1))) > 0;
    if (reversed) {
      Collections.reverse(ring);
    }

    int far = 1;
    double farDistance = -1;
    for (int i = 1; i < m; i++) {
      double d = Math.hypot(ring.get(i)[0] - ring.get(0)[0], ring.get(i)[1] - ring.get(0)[1]);
      if (d > farDistance) {
        farDistance = d;
        far = i;
      }
    }
    List<double[]> joined =
        new ArrayList<>(DouglasPeucker.simplify(ring.subList(0, far + 1), tolerance));
    List<double[]> second = DouglasPeucker.simplify(ring.subList(far, m + 1), tolerance);
    joined.addAll(second.subList(1, second.size()));
    if (joined.size() < 4) {
      joined = ring;
    }
    if (reversed) {
      joined = new ArrayList<>(joined);
      Collections.reverse(joined);
    }
    return GeoPoints.roundLine(joined);
  }
}
