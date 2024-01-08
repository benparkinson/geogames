package com.parkinson.benjamin.geogames.helper;

import java.util.Optional;
import java.util.function.BiFunction;

public class OptionalHelper {

  public static <T, U, R> Optional<R> map(
      Optional<T> t, Optional<U> u, BiFunction<T, U, R> biFunction) {
    return t.flatMap(tVal -> u.map(uVal -> biFunction.apply(tVal, uVal)));
  }
}
