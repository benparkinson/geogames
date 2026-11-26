package com.parkinson.benjamin.geogames.service.ratelimit;

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

  private GameCreationGuard guard(int gamesPerWindow, long maxLive) {
    return new GameCreationGuard(
        repository,
        Clock.systemUTC(),
        metrics,
        gamesPerWindow,
        Duration.ofMinutes(10),
        10,
        1,
        maxLive);
  }

  @Test
  void anAllowedGameIsNotCounted() {
    guard(1, 100).check(new MockHttpServletRequest());

    verifyNoInteractions(metrics);
  }

  @Test
  void aRateLimitedGameIsCounted() {
    GameCreationGuard guard = guard(1, 100);
    guard.check(new MockHttpServletRequest());

    assertThatThrownBy(() -> guard.check(new MockHttpServletRequest()))
        .isInstanceOf(TooManyGamesException.class);

    verify(metrics).gameRejected(RejectReason.RATE_LIMITED);
    verifyNoMoreInteractions(metrics);
  }

  @Test
  void aGameRefusedForCapacityIsCounted() {
    when(repository.count()).thenReturn(100L);

    assertThatThrownBy(() -> guard(0, 100).check(new MockHttpServletRequest()))
        .isInstanceOf(GameCapacityException.class);

    verify(metrics).gameRejected(RejectReason.CAPACITY);
    verifyNoMoreInteractions(metrics);
  }
}
