package com.parkinson.benjamin.geogames.service.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class ClientRateLimiterTest {

  /** A clock the test can move. */
  private static final class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-11-23T10:00:00Z");

    void advance(Duration d) {
      now = now.plus(d);
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private final MutableClock clock = new MutableClock();

  private ClientRateLimiter limiter(int limit, int maxClients) {
    return new ClientRateLimiter(clock, limit, Duration.ofMinutes(10), maxClients);
  }

  @Test
  void allowsTheLimitAndRefusesTheNextWithTheWait() {
    ClientRateLimiter limiter = limiter(3, 100);
    for (int i = 0; i < 3; i++) {
      assertThat(limiter.tryAcquire("1.2.3.4")).isEmpty();
    }
    clock.advance(Duration.ofMinutes(4));

    OptionalLong wait = limiter.tryAcquire("1.2.3.4");

    assertThat(wait).hasValue(360);
  }

  @Test
  void aNewWindowResetsTheCount() {
    ClientRateLimiter limiter = limiter(1, 100);
    assertThat(limiter.tryAcquire("a")).isEmpty();
    assertThat(limiter.tryAcquire("a")).isPresent();

    clock.advance(Duration.ofMinutes(10));

    assertThat(limiter.tryAcquire("a")).isEmpty();
  }

  @Test
  void addressesAreCountedSeparately() {
    ClientRateLimiter limiter = limiter(1, 100);
    assertThat(limiter.tryAcquire("a")).isEmpty();

    assertThat(limiter.tryAcquire("b")).isEmpty();
    assertThat(limiter.tryAcquire("a")).isPresent();
  }

  @Test
  void aFullMapDropsExpiredEntriesThenFailsOpen() {
    ClientRateLimiter limiter = limiter(1, 2);
    limiter.tryAcquire("a");
    limiter.tryAcquire("b");

    // Full of live entries: a third client is let through, and is not tracked.
    assertThat(limiter.tryAcquire("c")).isEmpty();
    assertThat(limiter.tryAcquire("c")).isEmpty();
    assertThat(limiter.keys()).hasSize(2);

    // Once the old windows expire there is room again, so c is tracked and limited.
    clock.advance(Duration.ofMinutes(11));
    assertThat(limiter.tryAcquire("c")).isEmpty();
    assertThat(limiter.tryAcquire("c")).isPresent();
    assertThat(limiter.keys()).hasSize(1);
  }

  @Test
  void expiredWindowsAreSweptAboutOnceAMinute() {
    ClientRateLimiter limiter = limiter(1, 100);
    limiter.tryAcquire("a");
    clock.advance(Duration.ofMinutes(11));

    limiter.tryAcquire("b");

    assertThat(limiter.keys()).hasSize(1);
  }

  @Test
  void theKeysAreNotTheAddress() {
    ClientRateLimiter limiter = limiter(1, 100);
    limiter.tryAcquire("203.0.113.7");

    assertThat(limiter.keys()).hasSize(1).noneMatch(key -> key.contains("203.0.113.7"));
  }
}
