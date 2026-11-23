package com.parkinson.benjamin.geogames.service.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.dao.GameRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LiveGameCapTest {

  private final GameRepository repository = mock(GameRepository.class);
  private final Instant start = Instant.parse("2026-11-23T10:00:00Z");

  @Test
  void countsOnceEveryMinuteAndRefusesAtTheCap() {
    when(repository.count()).thenReturn(9L, 10L);
    Clock clock = Mockito.mock(Clock.class);
    when(clock.millis())
        .thenReturn(
            start.toEpochMilli(),
            start.plusSeconds(30).toEpochMilli(),
            start.plus(Duration.ofSeconds(61)).toEpochMilli());
    LiveGameCap cap = new LiveGameCap(repository, clock, 10);

    assertThat(cap.isFull()).isFalse();
    assertThat(cap.isFull()).isFalse();
    verify(repository, times(1)).count();

    assertThat(cap.isFull()).isTrue();
    verify(repository, times(2)).count();
  }

  @Test
  void aCountBelowTheCapIsNotFull() {
    when(repository.count()).thenReturn(0L);
    Clock clock = Clock.fixed(start, ZoneOffset.UTC);

    assertThat(new LiveGameCap(repository, clock, 1).isFull()).isFalse();
  }
}
