package com.parkinson.benjamin.geogames.loaders.features;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GeoPointsTest {

  @Test
  void roundsToFourDecimals() {
    assertThat(GeoPoints.round4(1.234567)).isEqualTo(1.2346);
    assertThat(GeoPoints.round4(-0.00004)).isZero();
  }

  @Test
  void roundLineDropsAPointEqualToThePreviousOne() {
    List<double[]> line =
        GeoPoints.roundLine(
            List.of(
                new double[] {1.00001, 2.00001},
                new double[] {1.00002, 2.00002},
                new double[] {3, 4},
                new double[] {1.00001, 2.00001}));

    assertThat(line).hasSize(3);
    assertThat(line.get(0)).containsExactly(1.0, 2.0);
    assertThat(line.get(1)).containsExactly(3.0, 4.0);
    assertThat(line.get(2)).containsExactly(1.0, 2.0);
  }

  @Test
  void toPositionsPairsLongitudeAndLatitude() {
    assertThat(GeoPoints.toPositions(List.of(new double[] {1, 2}, new double[] {3, 4})))
        .containsExactly(List.of(1.0, 2.0), List.of(3.0, 4.0));
  }
}
