package com.parkinson.benjamin.geogames.model.geojson;

import java.util.AbstractList;
import java.util.RandomAccess;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * One [longitude, latitude] pair read from JSON, held as two doubles: a third of the heap of an
 * ArrayList of two Doubles, and the app keeps hundreds of thousands of them. Immutable.
 */
@JsonSerialize(using = PositionSerializer.class)
public final class Position extends AbstractList<Double> implements RandomAccess {

  private final double x;
  private final double y;

  public Position(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public double x() {
    return x;
  }

  public double y() {
    return y;
  }

  @Override
  public Double get(int index) {
    return switch (index) {
      case 0 -> x;
      case 1 -> y;
      default -> throw new IndexOutOfBoundsException("Index: " + index + ", Size: 2");
    };
  }

  @Override
  public int size() {
    return 2;
  }
}
