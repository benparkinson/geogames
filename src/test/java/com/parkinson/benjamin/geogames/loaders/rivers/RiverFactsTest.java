package com.parkinson.benjamin.geogames.loaders.rivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.loaders.features.CountryLocator;
import com.parkinson.benjamin.geogames.model.Coordinate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiverFactsTest {

  private static final List<List<double[]>> LINES =
      List.of(List.of(new double[] {0, 1}, new double[] {5, 1}));
  private static final CountryLocator.Result LOCATED =
      new CountryLocator.Result(List.of("A", "B"), 0);

  private static RiverOverride override(Boolean reverse, Coordinate source, Coordinate mouth) {
    return new RiverOverride(null, null, null, null, null, null, null, reverse, source, mouth);
  }

  @Test
  void usesTheComputedEndsWithoutAnOverride() {
    RiverFacts facts = RiverFacts.compute(LINES, null, LOCATED);

    assertThat(facts.source()).isEqualTo(new Coordinate(1, 0));
    assertThat(facts.mouth()).isEqualTo(new Coordinate(1, 5));
    assertThat(facts.flowConfidence()).isEqualTo(1.0);
    assertThat(facts.countries()).containsExactly("A", "B");
  }

  @Test
  void reverseFlowSwapsTheEnds() {
    RiverFacts facts = RiverFacts.compute(LINES, override(true, null, null), LOCATED);

    assertThat(facts.source()).isEqualTo(new Coordinate(1, 5));
    assertThat(facts.mouth()).isEqualTo(new Coordinate(1, 0));
  }

  @Test
  void explicitCoordinatesWin() {
    Coordinate source = new Coordinate(9, 9);
    Coordinate mouth = new Coordinate(8, 8);

    RiverFacts facts = RiverFacts.compute(LINES, override(true, source, mouth), LOCATED);

    assertThat(facts.source()).isEqualTo(source);
    assertThat(facts.mouth()).isEqualTo(mouth);
  }
}
