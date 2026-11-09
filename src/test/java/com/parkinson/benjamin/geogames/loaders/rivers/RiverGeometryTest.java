package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiverGeometryTest {

  /** A straight line along the equator, from lon 0 to lon {@code degrees}. */
  private static List<double[]> straight(double degrees) {
    return List.of(new double[] {0, 0}, new double[] {degrees / 2, 0}, new double[] {degrees, 0});
  }

  /** A line that swings by a whole degree at every point, so no tolerance can remove a point. */
  private static List<double[]> zigzag(int points, double step, double baseLat) {
    List<double[]> line = new ArrayList<>();
    for (int i = 0; i < points; i++) {
      line.add(new double[] {i * step, baseLat + (i % 2)});
    }
    return line;
  }

  @Test
  void aLineWithOnePointIsDropped() {
    List<List<double[]>> result =
        RiverGeometry.simplify(List.of(List.of(new double[] {1, 1}), straight(2)));

    assertThat(result).hasSize(1);
  }

  @Test
  void noLinesGiveNoLines() {
    assertThat(RiverGeometry.simplify(List.of())).isEmpty();
  }

  @Test
  void collinearPointsAreRemoved() {
    List<List<double[]>> result = RiverGeometry.simplify(List.of(straight(2)));

    assertThat(result).singleElement().satisfies(line -> assertThat(line).hasSize(2));
  }

  @Test
  void shortSideLinesAreDroppedButTheLongestLineIsKeptWhateverItsLength() {
    List<double[]> longest = straight(2);
    List<double[]> shortSide =
        List.of(new double[] {10, 10}, new double[] {10.01, 10}); // about 1 km
    List<double[]> longSide = List.of(new double[] {20, 20}, new double[] {21, 20}); // about 105 km

    assertThat(RiverGeometry.simplify(List.of(longest, shortSide, longSide))).hasSize(2);
    // Only one line, itself under the minimum, is still kept.
    assertThat(RiverGeometry.simplify(List.of(shortSide))).hasSize(1);
  }

  @Test
  void coordinatesAreRoundedToFourDecimalPlaces() {
    List<List<double[]>> result =
        RiverGeometry.simplify(
            List.of(List.of(new double[] {0.123456, 0.654321}, new double[] {1.5, 2.00004})));

    assertThat(result.get(0).get(0)).containsExactly(0.1235, 0.6543);
    assertThat(result.get(0).get(1)).containsExactly(1.5, 2.0);
  }

  @Test
  void pointsThatRoundToTheSamePointAreMerged() {
    List<double[]> line =
        List.of(new double[] {0, 0}, new double[] {0.00001, 0.00001}, new double[] {5, 5});

    assertThat(RiverGeometry.simplify(List.of(line)).get(0)).hasSize(2);
  }

  @Test
  void aLineThatCollapsesToOnePointIsDropped() {
    List<double[]> line = List.of(new double[] {3, 3}, new double[] {3.00001, 3.00001});

    assertThat(RiverGeometry.simplify(List.of(line))).isEmpty();
  }

  @Test
  void aWigglyLineIsSimplifiedMoreCoarselyUntilItFitsThePointCap() {
    // 2000 points swinging by 0.05 degrees: kept at the first tolerances, gone at 0.08.
    List<double[]> wiggly = new ArrayList<>();
    for (int i = 0; i < 2000; i++) {
      wiggly.add(new double[] {i * 0.001, (i % 2) * 0.05});
    }

    List<List<double[]>> result = RiverGeometry.simplify(List.of(wiggly));

    assertThat(RiverGeometry.pointCount(result)).isLessThanOrEqualTo(RiverGeometry.MAX_POINTS);
    assertThat(result).hasSize(1);
  }

  @Test
  void ifSimplifyingIsNotEnoughTheShortestLinesAreDroppedUntilTheCapHolds() {
    List<List<double[]>> lines = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      lines.add(zigzag(100, 0.1 + 0.01 * i, i * 5));
    }
    double longestKm = RiverGeometry.lengthKm(List.of(lines.get(9)));

    List<List<double[]>> result = RiverGeometry.simplify(lines);

    assertThat(RiverGeometry.pointCount(result)).isLessThanOrEqualTo(RiverGeometry.MAX_POINTS);
    assertThat(result.size()).isLessThan(10);
    assertThat(RiverGeometry.lengthKm(result)).isGreaterThanOrEqualTo(longestKm * 0.99);
  }

  @Test
  void lengthIsTheSumOfGreatCircleDistances() {
    assertThat(RiverGeometry.lengthKm(List.of(straight(1), straight(2))))
        .isCloseTo(3 * 111.195, within(0.1));
    assertThat(RiverGeometry.lengthKm(List.of())).isZero();
  }

  @Test
  void pointCountAddsUpAllLines() {
    assertThat(RiverGeometry.pointCount(List.of(straight(1), zigzag(4, 1, 0)))).isEqualTo(7);
  }

  private static List<double[]> line(double lon1, double lat1, double lon2, double lat2) {
    return List.of(new double[] {lon1, lat1}, new double[] {lon2, lat2});
  }

  @Test
  void mainPieceOfNoLinesOrOneLineIsTheLinesThemselves() {
    assertThat(RiverGeometry.mainPiece(List.of())).isEmpty();
    List<double[]> only = line(0, 0, 1, 0);
    assertThat(RiverGeometry.mainPiece(List.of(only))).containsExactly(only);
  }

  @Test
  void mainPieceIgnoresLinesWithFewerThanTwoPoints() {
    List<double[]> only = line(0, 0, 1, 0);

    assertThat(RiverGeometry.mainPiece(List.of(List.of(new double[] {5, 5}), only)))
        .containsExactly(only);
  }

  @Test
  void mainPieceJoinsLinesAtEitherEndAcrossSmallGapsAndKeepsTheLongestPiece() {
    // a and b touch end to start, c starts about 11 km from b's end, d starts where a starts.
    List<double[]> a = line(0, 0, 1, 0);
    List<double[]> b = line(1, 0, 2, 0);
    List<double[]> c = line(2.1, 0, 3, 0);
    List<double[]> d = line(0, 0, 0, -1);
    // A longer line than any one of them, but far away and alone.
    List<double[]> far = line(0, 20, 2.5, 20);

    assertThat(RiverGeometry.mainPiece(List.of(a, far, b, c, d))).containsExactly(a, b, c, d);
  }

  @Test
  void mainPieceKeepsTheFarPieceWhenItIsTheLongest() {
    List<double[]> a = line(0, 0, 1, 0);
    List<double[]> b = line(1, 0, 2, 0);
    List<double[]> far = line(0, 20, 5, 20);

    assertThat(RiverGeometry.mainPiece(List.of(a, b, far))).containsExactly(far);
  }
}
