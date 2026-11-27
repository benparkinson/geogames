package com.parkinson.benjamin.geogames.service.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics;
import com.parkinson.benjamin.geogames.service.metrics.GameMetrics.RejectReason;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GameCreationGuardTest {

  private final GameRepository repository = mock(GameRepository.class);

  private final GameMetrics metrics = mock(GameMetrics.class);

  private GameCreationGuard guard(int perClient, int global, long maxLive) {
    return new GameCreationGuard(
        repository,
        Clock.systemUTC(),
        metrics,
        perClient,
        Duration.ofMinutes(10),
        100,
        1,
        global,
        maxLive);
  }

  private static MockHttpServletRequest from(String address) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", address);
    return request;
  }

  @Test
  void theGlobalRateRefusesWithTheCapacityException() {
    GameCreationGuard guard = guard(0, 1, 1000);
    guard.check(from("203.0.113.1"));

    assertThatThrownBy(() -> guard.check(from("203.0.113.2")))
        .isInstanceOf(GameCapacityException.class)
        .extracting(e -> ((GameCapacityException) e).retryAfterSeconds())
        .satisfies(s -> assertThat((Long) s).isBetween(1L, 60L));

    verify(metrics).gameRejected(RejectReason.GLOBAL_RATE);
    verifyNoMoreInteractions(metrics);
  }

  @Test
  void theClientLimitComesBeforeTheGlobalRate() {
    GameCreationGuard guard = guard(1, 1, 1000);
    guard.check(from("203.0.113.1"));

    assertThatThrownBy(() -> guard.check(from("203.0.113.1")))
        .isInstanceOf(TooManyGamesException.class);
  }

  @Test
  void theLiveCapAppliesWhenTheGlobalRateIsOff() {
    when(repository.count()).thenReturn(5L);

    assertThatThrownBy(() -> guard(0, 0, 5).check(from("203.0.113.1")))
        .isInstanceOf(GameCapacityException.class);
  }

  @Test
  void anAllowedGameIsNotCounted() {
    guard(1, 0, 100).check(new MockHttpServletRequest());

    verifyNoInteractions(metrics);
  }

  @Test
  void aRateLimitedGameIsCounted() {
    GameCreationGuard guard = guard(1, 0, 100);
    guard.check(new MockHttpServletRequest());

    assertThatThrownBy(() -> guard.check(new MockHttpServletRequest()))
        .isInstanceOf(TooManyGamesException.class);

    verify(metrics).gameRejected(RejectReason.RATE_LIMITED);
    verifyNoMoreInteractions(metrics);
  }

  @Test
  void aGameRefusedForCapacityIsCounted() {
    when(repository.count()).thenReturn(100L);

    assertThatThrownBy(() -> guard(0, 0, 100).check(new MockHttpServletRequest()))
        .isInstanceOf(GameCapacityException.class);

    verify(metrics).gameRejected(RejectReason.CAPACITY);
    verifyNoMoreInteractions(metrics);
  }
}
