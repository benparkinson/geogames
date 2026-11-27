package com.parkinson.benjamin.geogames.service.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class GlobalCreationRateTest {

  private static class MutableClock extends Clock {
    private long millis = Instant.parse("2026-11-26T10:00:00Z").toEpochMilli();

    @Override
    public long millis() {
      return millis;
    }

    @Override
    public Instant instant() {
      return Instant.ofEpochMilli(millis);
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }
  }

  @Test
  void theCreationOverTheRateIsRefusedWithTheSecondsLeftInTheMinute() {
    MutableClock clock = new MutableClock();
    GlobalCreationRate rate = new GlobalCreationRate(clock, 200);
    for (int i = 0; i < 200; i++) {
      assertThat(rate.tryAcquire()).isEmpty();
    }
    clock.millis += 20_000;

    assertThat(rate.tryAcquire()).isEqualTo(OptionalLong.of(40));
  }

  @Test
  void aNewMinuteAllowsCreationAgain() {
    MutableClock clock = new MutableClock();
    GlobalCreationRate rate = new GlobalCreationRate(clock, 2);
    rate.tryAcquire();
    rate.tryAcquire();
    assertThat(rate.tryAcquire()).isPresent();

    clock.millis += 60_000;

    assertThat(rate.tryAcquire()).isEmpty();
  }
}
