package com.parkinson.benjamin.geogames.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

class OptionalHelperTest {

  @Test
  void appliesFunctionWhenBothPresent() {
    Optional<String> result = OptionalHelper.map(Optional.of("a"), Optional.of(2), (s, i) -> s + i);

    assertThat(result).contains("a2");
  }

  @Test
  @SuppressWarnings("unchecked")
  void returnsEmptyAndSkipsFunctionWhenFirstIsEmpty() {
    BiFunction<String, Integer, String> function = mock(BiFunction.class);

    assertThat(OptionalHelper.map(Optional.<String>empty(), Optional.of(2), function)).isEmpty();
    verifyNoInteractions(function);
  }

  @Test
  @SuppressWarnings("unchecked")
  void returnsEmptyAndSkipsFunctionWhenSecondIsEmpty() {
    BiFunction<String, Integer, String> function = mock(BiFunction.class);

    assertThat(OptionalHelper.map(Optional.of("a"), Optional.<Integer>empty(), function)).isEmpty();
    verifyNoInteractions(function);
  }
}
