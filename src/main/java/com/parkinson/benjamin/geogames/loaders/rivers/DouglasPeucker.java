package com.parkinson.benjamin.geogames.loaders.rivers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Douglas-Peucker line simplification in plain degree space. Endpoints are always kept. */
public final class DouglasPeucker {

  private DouglasPeucker() {}

  public static List<double[]> simplify(List<double[]> points, double tolerance) {
    int n = points.size();
    if (n <= 2) {
      return new ArrayList<>(points);
    }
    boolean[] keep = new boolean[n];
    keep[0] = true;
    keep[n - 1] = true;

    Deque<int[]> stack = new ArrayDeque<>();
    stack.push(new int[] {0, n - 1});
    while (!stack.isEmpty()) {
      int[] range = stack.pop();
      int first = range[0];
      int last = range[1];
      double maxDistance = -1;
      int index = -1;
      for (int i = first + 1; i < last; i++) {
        double d = distance(points.get(i), points.get(first), points.get(last));
        if (d > maxDistance) {
          maxDistance = d;
          index = i;
        }
      }
      if (index >= 0 && maxDistance > tolerance) {
        keep[index] = true;
        stack.push(new int[] {first, index});
        stack.push(new int[] {index, last});
      }
    }

    List<double[]> result = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      if (keep[i]) {
        result.add(points.get(i));
      }
    }
    return result;
  }

  /** Distance from p to the segment a-b. */
  private static double distance(double[] p, double[] a, double[] b) {
    double dx = b[0] - a[0];
    double dy = b[1] - a[1];
    double lengthSquared = dx * dx + dy * dy;
    double t = 0;
    if (lengthSquared > 0) {
      t = Math.max(0, Math.min(1, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / lengthSquared));
    }
    return Math.hypot(p[0] - (a[0] + t * dx), p[1] - (a[1] + t * dy));
  }
}
