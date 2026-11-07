package com.parkinson.benjamin.geogames.loaders.rivers;

import java.util.Arrays;
import java.util.List;

/**
 * Finds where a river starts and where it ends from its lines, which often don't touch.
 *
 * <p>Nodes are the ends of each line. Each line joins its two ends, and ends of different lines
 * within {@link #GAP_KM} of each other are joined too. The river's ends are the two nodes furthest
 * apart by shortest path. Which of the two is the source comes from the direction the lines were
 * digitised in: walking the path, each line walked start-to-end votes for the first node and each
 * walked end-to-start votes against.
 */
public final class RiverEnds {

  static final double GAP_KM = 25;

  /** Points are {lon, lat}. {@code confidence} is 1 when every line agrees on the direction. */
  public record Ends(double[] source, double[] mouth, double confidence) {}

  private RiverEnds() {}

  public static Ends find(List<List<double[]>> lines) {
    if (lines.isEmpty()) {
      throw new IllegalArgumentException("A river needs at least one line");
    }
    int n = lines.size() * 2;
    double[][] points = new double[n][];
    for (int i = 0; i < lines.size(); i++) {
      List<double[]> line = lines.get(i);
      points[2 * i] = line.get(0);
      points[2 * i + 1] = line.get(line.size() - 1);
    }

    double[][] weight = new double[n][n];
    for (double[] row : weight) {
      Arrays.fill(row, Double.POSITIVE_INFINITY);
    }
    for (int i = 0; i < lines.size(); i++) {
      double km = RiverGeometry.lineKm(lines.get(i));
      weight[2 * i][2 * i + 1] = km;
      weight[2 * i + 1][2 * i] = km;
    }
    for (int a = 0; a < n; a++) {
      for (int b = a + 1; b < n; b++) {
        if (a / 2 != b / 2) {
          double km = RiverGeometry.haversineKm(points[a], points[b]);
          if (km <= GAP_KM) {
            weight[a][b] = km;
            weight[b][a] = km;
          }
        }
      }
    }

    double best = -1;
    int bestFrom = 0;
    int bestTo = n > 1 ? 1 : 0;
    int[] bestPrev = null;
    for (int from = 0; from < n; from++) {
      int[] previous = new int[n];
      double[] distance = dijkstra(weight, from, previous);
      for (int to = from + 1; to < n; to++) {
        if (distance[to] < Double.POSITIVE_INFINITY && distance[to] > best) {
          best = distance[to];
          bestFrom = from;
          bestTo = to;
          bestPrev = previous;
        }
      }
    }

    double vote = 0;
    double lineKm = 0;
    int node = bestTo;
    while (node != bestFrom) {
      int before = bestPrev[node];
      if (before / 2 == node / 2) {
        double km = weight[before][node];
        lineKm += km;
        vote += before % 2 == 0 ? km : -km;
      }
      node = before;
    }

    double[] source = points[bestFrom];
    double[] mouth = points[bestTo];
    if (vote < 0) {
      source = points[bestTo];
      mouth = points[bestFrom];
    }
    return new Ends(source, mouth, lineKm == 0 ? 0 : Math.abs(vote) / lineKm);
  }

  private static double[] dijkstra(double[][] weight, int from, int[] previous) {
    int n = weight.length;
    double[] distance = new double[n];
    boolean[] done = new boolean[n];
    Arrays.fill(distance, Double.POSITIVE_INFINITY);
    Arrays.fill(previous, -1);
    distance[from] = 0;
    for (int step = 0; step < n; step++) {
      int u = -1;
      for (int i = 0; i < n; i++) {
        if (!done[i] && (u == -1 || distance[i] < distance[u])) {
          u = i;
        }
      }
      if (u == -1 || distance[u] == Double.POSITIVE_INFINITY) {
        break;
      }
      done[u] = true;
      for (int v = 0; v < n; v++) {
        if (distance[u] + weight[u][v] < distance[v]) {
          distance[v] = distance[u] + weight[u][v];
          previous[v] = u;
        }
      }
    }
    return distance;
  }
}
