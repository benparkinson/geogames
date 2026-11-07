package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RandomPickerTest {

  private final List<Integer> items = List.of(1, 2, 3, 4, 5, 6);

  @Test
  void picksDistinctItemsFromTheInput() {
    List<Integer> picked = RandomPicker.pickDistinct(items, 4, new Random(1), "numbers");

    assertThat(picked).hasSize(4).doesNotHaveDuplicates().isSubsetOf(items);
  }

  @Test
  void canPickEveryItem() {
    assertThat(RandomPicker.pickDistinct(items, items.size(), new Random(1), "numbers"))
        .containsExactlyInAnyOrderElementsOf(items);
  }

  @Test
  void pickingNothingGivesAnEmptyList() {
    assertThat(RandomPicker.pickDistinct(items, 0, new Random(1), "numbers")).isEmpty();
    assertThat(RandomPicker.pickDistinct(List.<Integer>of(), 0, new Random(1), "numbers"))
        .isEmpty();
  }

  @Test
  void rejectsMoreItemsThanThereAre() {
    assertThatThrownBy(() -> RandomPicker.pickDistinct(items, 7, new Random(1), "numbers"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Can't give you 7 numbers, I only know about 6!");
  }

  @Test
  void leavesTheInputAlone() {
    List<Integer> input = new ArrayList<>(items);

    RandomPicker.pickDistinct(input, 3, new Random(1), "numbers");

    assertThat(input).containsExactlyElementsOf(items);
  }

  @Test
  void theSameSeedPicksTheSameItems() {
    assertThat(RandomPicker.pickDistinct(items, 3, new Random(42), "numbers"))
        .isEqualTo(RandomPicker.pickDistinct(items, 3, new Random(42), "numbers"));
  }

  @Test
  void returnsAListTheCallerMayChange() {
    List<Integer> picked = RandomPicker.pickDistinct(items, 2, new Random(1), "numbers");

    picked.add(99);

    assertThat(picked).hasSize(3);
  }
}
