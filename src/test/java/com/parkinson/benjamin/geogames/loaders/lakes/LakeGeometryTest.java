package com.parkinson.benjamin.geogames.loaders.lakes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LakeGeometryTest {

  /** A closed square ring with its lower-left corner at (lon, lat). */
  private static List<double[]> square(double lon, double lat, double size) {
    return List.of(
        new double[] {lon, lat},
        new double[] {lon + size, lat},
        new double[] {lon + size, lat + size},
        new double[] {lon, lat + size},
        new double[] {lon, lat});
  }

  private static List<List<double[]>> polygon(List<double[]>... rings) {
    return List.of(rings);
  }

  @Test
  void aSquareDegreeAtTheEquatorIsAboutTwelveThousandKm2() {
    double area = LakeGeometry.areaKm2(List.of(polygon(square(0, 0, 1))));

    assertThat(area).isCloseTo(111.32 * 110.57, within(50.0));
  }

  @Test
  void aSquareDegreeIsSmallerAwayFromTheEquator() {
    double equator = LakeGeometry.areaKm2(List.of(polygon(square(0, 0, 1))));
    double north = LakeGeometry.areaKm2(List.of(polygon(square(0, 60, 1))));

    assertThat(north).isCloseTo(equator / 2, within(equator * 0.02));
  }

  @Test
  void holesAreSubtracted() {
    double whole = LakeGeometry.areaKm2(List.of(polygon(square(0, 0, 1))));
    double holed = LakeGeometry.areaKm2(List.of(polygon(square(0, 0, 1), square(0.25, 0.25, 0.5))));

    assertThat(holed).isCloseTo(whole * 0.75, within(whole * 0.01));
  }

  @Test
  void smallIslandsAndStrayFragmentsAreDropped() {
    List<double[]> lake = square(0, 0, 2);
    List<double[]> bigIsland = square(0.5, 0.5, 0.5); // 6% of the lake: kept
    List<double[]> tinyIsland = square(1.5, 1.5, 0.1); // 0.25%: dropped
    List<double[]> fragment = square(10, 10, 0.1); // 0.25% of the lake: dropped
    List<double[]> second = square(5, 5, 1); // 25%: kept

    List<List<List<double[]>>> result =
        LakeGeometry.simplify(
            List.of(polygon(lake, bigIsland, tinyIsland), polygon(fragment), polygon(second)));

    assertThat(result).hasSize(2);
    assertThat(result.get(0)).hasSize(2); // lake and the big island
    assertThat(result.get(1)).hasSize(1);
  }

  @Test
  void theMainBodyComesFirst() {
    List<List<List<double[]>>> result =
        LakeGeometry.simplify(List.of(polygon(square(5, 5, 1)), polygon(square(0, 0, 2))));

    assertThat(result.get(0).get(0).get(0)).containsExactly(0.0, 0.0);
  }

  @Test
  void theCapHoldsOnAManyPointCircleAndRingsStayClosed() {
    List<double[]> circle = new ArrayList<>();
    int n = 20_000;
    for (int i = 0; i < n; i++) {
      double angle = 2 * Math.PI * i / n;
      circle.add(new double[] {Math.cos(angle) * 300, Math.sin(angle) * 300});
    }
    circle.add(circle.get(0));

    List<List<List<double[]>>> result = LakeGeometry.simplify(List.of(polygon(circle)));

    assertThat(LakeGeometry.pointCount(result)).isBetween(4, LakeGeometry.MAX_POINTS);
    for (List<List<double[]>> polygon : result) {
      for (List<double[]> ring : polygon) {
        assertThat(ring.size()).isGreaterThanOrEqualTo(4);
        assertThat(ring.get(0)).containsExactly(ring.get(ring.size() - 1));
      }
    }
  }

  @Test
  void aDegenerateOuterRingDropsThePolygon() {
    List<double[]> sliver = List.of(new double[] {0, 0}, new double[] {1, 0}, new double[] {0, 0});

    assertThat(LakeGeometry.simplify(List.of(polygon(sliver)))).isEmpty();
  }

  @Test
  void coordinatesAreRoundedToFourDecimals() {
    List<double[]> ring =
        List.of(
            new double[] {0.123456789, 0.987654321},
            new double[] {1.123456789, 0.987654321},
            new double[] {1.123456789, 1.987654321},
            new double[] {0.123456789, 1.987654321},
            new double[] {0.123456789, 0.987654321});

    List<List<List<double[]>>> result = LakeGeometry.simplify(List.of(polygon(ring)));

    assertThat(result.get(0).get(0).get(0)).containsExactly(0.1235, 0.9877);
  }
}
