package com.parkinson.benjamin.geogames.loaders.features;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GeoTest {

  private static List<double[]> square(double west, double south, double east, double north) {
    return List.of(
        new double[] {west, south},
        new double[] {east, south},
        new double[] {east, north},
        new double[] {west, north});
  }

  @Test
  void aSquareDegreeAtTheEquatorIsAboutTwelveThousandKm2() {
    assertThat(Geo.ringAreaKm2(square(0, 0, 1, 1))).isBetween(12_300.0, 12_400.0);
  }

  @Test
  void theWindingAndAClosingPointDontMatter() {
    List<double[]> ring = square(10, 40, 12, 42);
    List<double[]> reversed = new java.util.ArrayList<>(ring);
    java.util.Collections.reverse(reversed);
    List<double[]> closed = new java.util.ArrayList<>(ring);
    closed.add(ring.get(0));

    double area = Geo.ringAreaKm2(ring);

    assertThat(Geo.ringAreaKm2(reversed)).isEqualTo(area);
    assertThat(Geo.ringAreaKm2(closed)).isEqualTo(area);
  }

  @Test
  void aBandNearThePoleIsSmallerThanOneAtTheEquator() {
    assertThat(Geo.ringAreaKm2(square(0, 80, 10, 81)))
        .isLessThan(Geo.ringAreaKm2(square(0, 0, 10, 1)) / 4);
  }

  @Test
  void aRingAcrossTheAntimeridianCountsTheShortWay() {
    assertThat(Geo.ringAreaKm2(square(179, 0, -179, 1)))
        .isEqualTo(Geo.ringAreaKm2(square(0, 0, 2, 1)), org.assertj.core.data.Offset.offset(1.0));
  }
}
