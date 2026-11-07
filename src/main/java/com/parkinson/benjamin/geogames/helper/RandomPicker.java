package com.parkinson.benjamin.geogames.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RandomPicker {

  private RandomPicker() {}

  /**
   * Picks {@code count} different items at random. The input list is left alone.
   *
   * @param noun the plural name of the items, for the error message
   * @throws IllegalArgumentException if the list has fewer than {@code count} items
   */
  public static <T> List<T> pickDistinct(List<T> items, int count, Random random, String noun) {
    if (items.size() < count) {
      throw new IllegalArgumentException(
          "Can't give you %d %s, I only know about %d!".formatted(count, noun, items.size()));
    }

    List<T> shuffled = new ArrayList<>(items);
    Collections.shuffle(shuffled, random);
    return new ArrayList<>(shuffled.subList(0, count));
  }
}
