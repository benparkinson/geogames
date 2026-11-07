package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DouglasPeuckerTest {

  private static double[] p(double x, double y) {
    return new double[] {x, y};
  }

  @Test
  void aStraightLineCollapsesToItsTwoEndpoints() {
    List<double[]> line = List.of(p(0, 0), p(1, 1), p(2, 2), p(3, 3), p(4, 4));

    List<double[]> result = DouglasPeucker.simplify(line, 0.01);

    assertThat(result).containsExactly(line.get(0), line.get(4));
  }

  @Test
  void aZigZagLargerThanTheToleranceIsKept() {
    List<double[]> line = List.of(p(0, 0), p(1, 1), p(2, 0), p(3, 1), p(4, 0));

    assertThat(DouglasPeucker.simplify(line, 0.1)).hasSize(5);
  }

  @Test
  void aZigZagSmallerThanTheToleranceIsRemoved() {
    List<double[]> line = List.of(p(0, 0), p(1, 0.01), p(2, 0), p(3, 0.01), p(4, 0));

    assertThat(DouglasPeucker.simplify(line, 0.1)).hasSize(2);
  }

  @Test
  void endpointsAreAlwaysKept() {
    List<double[]> line = List.of(p(0, 0), p(1, 5), p(2, 0.001), p(3, 0));

    List<double[]> result = DouglasPeucker.simplify(line, 100);

    assertThat(result.get(0)).isSameAs(line.get(0));
    assertThat(result.get(result.size() - 1)).isSameAs(line.get(3));
  }

  @Test
  void aClosedLoopKeepsItsFarthestPoint() {
    List<double[]> loop = List.of(p(0, 0), p(5, 5), p(0, 0));

    assertThat(DouglasPeucker.simplify(loop, 0.1)).hasSize(3);
  }
}
