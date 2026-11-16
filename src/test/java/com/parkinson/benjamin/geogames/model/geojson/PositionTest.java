package com.parkinson.benjamin.geogames.model.geojson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PositionTest {

  private final Position position = new Position(1.5, 2.5);

  @Test
  void holdsTwoValues() {
    assertThat(position.size()).isEqualTo(2);
    assertThat(position.get(0)).isEqualTo(1.5);
    assertThat(position.get(1)).isEqualTo(2.5);
    assertThat(position.x()).isEqualTo(1.5);
    assertThat(position.y()).isEqualTo(2.5);
  }

  @Test
  void anIndexOutOfRangeThrows() {
    assertThatThrownBy(() -> position.get(2)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> position.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void equalsAListOfTheSameDoublesInBothDirections() {
    List<Double> plain = List.of(1.5, 2.5);

    assertThat(position.equals(plain)).isTrue();
    assertThat(plain.equals(position)).isTrue();
    assertThat(position.hashCode()).isEqualTo(plain.hashCode());
    assertThat(position.equals(List.of(1.5, 9.0))).isFalse();
    assertThat(position.equals(List.of(1.5))).isFalse();
  }

  @Test
  void isImmutable() {
    assertThatThrownBy(() -> position.set(0, 3.0))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> position.add(3.0)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> position.remove(0)).isInstanceOf(UnsupportedOperationException.class);
  }
}
