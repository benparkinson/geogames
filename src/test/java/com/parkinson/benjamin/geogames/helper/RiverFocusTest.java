package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Bounds;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiverFocusTest {

  private static void assertContains(Bounds outer, Bounds inner) {
    assertThat(outer.union(inner)).isEqualTo(outer);
  }

  @Test
  void similarCountriesGiveTheUnionOfTheirFocusesAndTheRiver() {
    Bounds one = new Bounds(0, 0, 10, 10);
    Bounds two = new Bounds(8, 2, 20, 12);
    Bounds river = new Bounds(5, 4, 12, 8);

    assertThat(RiverFocus.of(river, List.of(one, two))).isEqualTo(one.union(two).union(river));
  }

  @Test
  void rhoneKeepsFranceWhole() {
    Bounds france = new Bounds(-4.8, 42.3, 8.2, 51.1);
    Bounds switzerland = new Bounds(5.9, 45.8, 10.5, 47.8);
    Bounds river = new Bounds(4.5, 43.3, 8.5, 46.6);

    assertContains(RiverFocus.of(river, List.of(france, switzerland)), france);
  }

  @Test
  void dnieperCapsRussiaButKeepsUkraineAndBelarusWhole() {
    Bounds russia = new Bounds(27.3, 41.2, 180, 77.7);
    Bounds ukraine = new Bounds(22.1, 44.4, 40.2, 52.4);
    Bounds belarus = new Bounds(23.2, 51.3, 32.8, 56.2);
    Bounds river = new Bounds(30, 46.5, 36, 56);

    Bounds result = RiverFocus.of(river, List.of(russia, ukraine, belarus));

    assertThat(result.east()).isLessThan(100);
    assertContains(result, ukraine);
    assertContains(result, belarus);
  }

  @Test
  void amurKeepsRussiaWhole() {
    Bounds russia = new Bounds(27.3, 41.2, 180, 77.7);
    Bounds china = new Bounds(73.6, 20.2, 134.8, 53.6);
    Bounds river = new Bounds(108, 43, 141, 53.5);

    assertContains(RiverFocus.of(river, List.of(russia, china)), russia);
  }

  @Test
  void alwaysContainsTheRiver() {
    Bounds huge = new Bounds(-170, 20, -60, 70);
    Bounds small = new Bounds(-100, 25, -95, 30);
    Bounds river = new Bounds(-105, 26, -97, 37);

    assertContains(RiverFocus.of(river, List.of(huge, small)), river);
  }
}
