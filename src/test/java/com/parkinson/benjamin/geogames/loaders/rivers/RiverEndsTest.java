package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiverEndsTest {

  private static List<double[]> line(double... lonLatPairs) {
    List<double[]> points = new ArrayList<>();
    for (int i = 0; i < lonLatPairs.length; i += 2) {
      points.add(new double[] {lonLatPairs[i], lonLatPairs[i + 1]});
    }
    return points;
  }

  @Test
  void aSingleLineStartsAtItsFirstPoint() {
    RiverEnds.Ends ends = RiverEnds.find(List.of(line(0, 0, 3, 0, 5, 0)));

    assertThat(ends.source()).containsExactly(0, 0);
    assertThat(ends.mouth()).containsExactly(5, 0);
    assertThat(ends.confidence()).isEqualTo(1.0);
  }

  @Test
  void aLineDigitisedTheOtherWayStartsAtItsFirstPointToo() {
    RiverEnds.Ends ends = RiverEnds.find(List.of(line(5, 0, 2, 0, 0, 0)));

    assertThat(ends.source()).containsExactly(5, 0);
    assertThat(ends.mouth()).containsExactly(0, 0);
  }

  @Test
  void twoLinesWithASmallGapAreJoined() {
    // 0.09 degrees of longitude at the equator is about 10 km.
    RiverEnds.Ends ends = RiverEnds.find(List.of(line(0, 0, 5, 0), line(5.09, 0, 8, 0)));

    assertThat(ends.source()).containsExactly(0, 0);
    assertThat(ends.mouth()).containsExactly(8, 0);
    assertThat(ends.confidence()).isEqualTo(1.0);
  }

  @Test
  void twoLinesWithABigGapAreNotJoinedSoTheLongerOneWins() {
    // 0.36 degrees is about 40 km.
    RiverEnds.Ends ends = RiverEnds.find(List.of(line(5.36, 0, 7, 0), line(0, 0, 5, 0)));

    assertThat(ends.source()).containsExactly(0, 0);
    assertThat(ends.mouth()).containsExactly(5, 0);
  }

  @Test
  void aBranchedRiverTakesTheLongestPath() {
    List<double[]> a = line(0, 0, 5, 0);
    List<double[]> b = line(5, 0, 10, 0);
    List<double[]> shortBranch = line(5, 0, 5, 2);

    RiverEnds.Ends ends = RiverEnds.find(List.of(a, shortBranch, b));

    assertThat(ends.source()).containsExactly(0, 0);
    assertThat(ends.mouth()).containsExactly(10, 0);
  }

  @Test
  void linesDigitisedInOppositeDirectionsGiveLowerConfidence() {
    // The first line runs with the chain (about 667 km), the second against it (about 556 km).
    RiverEnds.Ends ends = RiverEnds.find(List.of(line(0, 0, 6, 0), line(11, 0, 6, 0)));

    assertThat(ends.source()).containsExactly(0, 0);
    assertThat(ends.mouth()).containsExactly(11, 0);
    assertThat(ends.confidence()).isCloseTo(111.0 / 1223.0, within(0.01));
  }

  @Test
  void aRiverNeedsALine() {
    assertThatThrownBy(() -> RiverEnds.find(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
