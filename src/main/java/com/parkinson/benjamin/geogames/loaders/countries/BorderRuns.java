package com.parkinson.benjamin.geogames.loaders.countries;

import static com.parkinson.benjamin.geogames.loaders.countries.CountryRings.edgeKey;
import static com.parkinson.benjamin.geogames.loaders.countries.CountryRings.end;
import static com.parkinson.benjamin.geogames.loaders.countries.CountryRings.vertexKey;

import com.parkinson.benjamin.geogames.loaders.countries.CountryRings.Ring;
import com.parkinson.benjamin.geogames.loaders.countries.CountryRings.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Labels each edge of a ring as coast, seam or a neighbour's name, and splits a ring into runs of
 * edges with the same label.
 */
final class BorderRuns {

  static final String COAST = "\u0000coast";
  static final String SEAM = "\u0000seam";

  private static final double SEAM_LON = 179.9999;

  /** A stretch of a ring whose edges all have one label. */
  record Run(String label, List<double[]> points) {}

  private BorderRuns() {}

  /**
   * The label of each edge: the other country that uses the edge, {@link #COAST} if none does, or
   * {@link #SEAM} for an edge along the antimeridian cut.
   */
  static String[] labels(Source source, Ring ring, Map<String, Set<String>> users) {
    String[] labels = new String[ring.size()];
    for (int i = 0; i < labels.length; i++) {
      double[] a = ring.points().get(i);
      double[] b = end(ring, i);
      if (Math.abs(a[0]) >= SEAM_LON && Math.abs(b[0]) >= SEAM_LON && a[0] * b[0] > 0) {
        labels[i] = SEAM;
        continue;
      }
      Set<String> using = users.get(edgeKey(ring, i));
      if (using.size() > 2) {
        throw new IllegalStateException(
            "More than two countries share an edge near %s: %s"
                .formatted(vertexKey(a), String.join(", ", using)));
      }
      labels[i] =
          using.size() == 2
              ? using.stream().filter(name -> !name.equals(source.name())).findFirst().orElse(COAST)
              : COAST;
    }
    return labels;
  }

  /**
   * Splits a ring into maximal runs of edges with the same label, starting at a label change. A
   * ring with one label throughout is one closed run (first point equals last).
   */
  static List<Run> runs(Ring ring, String[] labels) {
    int m = ring.size();
    int start = -1;
    for (int i = 0; i < m; i++) {
      if (!labels[i].equals(labels[(i + m - 1) % m])) {
        start = i;
        break;
      }
    }
    if (start < 0) {
      List<double[]> closed = new ArrayList<>(ring.points());
      closed.add(ring.points().get(0));
      return List.of(new Run(labels[0], closed));
    }
    List<Run> runs = new ArrayList<>();
    String label = null;
    List<double[]> points = null;
    for (int k = 0; k < m; k++) {
      int edge = (start + k) % m;
      if (!labels[edge].equals(label)) {
        if (points != null) {
          runs.add(new Run(label, points));
        }
        label = labels[edge];
        points = new ArrayList<>();
        points.add(ring.points().get(edge));
      }
      points.add(end(ring, edge));
    }
    runs.add(new Run(label, points));
    return runs;
  }
}
