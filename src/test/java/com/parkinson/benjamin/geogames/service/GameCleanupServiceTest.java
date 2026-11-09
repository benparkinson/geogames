package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.dao.GameRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class GameCleanupServiceTest {

  private final GameRepository repository = mock(GameRepository.class);
  private final GameCleanupService service =
      new GameCleanupService(repository, Duration.ofHours(24));

  @Test
  void deletesGamesCreatedBeforeNowMinusMaxAge() {
    Instant now = Instant.parse("2026-11-08T12:00:00Z");
    when(repository.deleteByCreatedAtBefore(Instant.parse("2026-11-07T12:00:00Z"))).thenReturn(3L);

    assertThat(service.deleteGamesOlderThanMaxAge(now)).isEqualTo(3);

    verify(repository).deleteByCreatedAtBefore(Instant.parse("2026-11-07T12:00:00Z"));
  }

  @Test
  void theScheduledRunDeletesGamesOlderThanTheMaxAge() {
    service.deleteOldGames();

    verify(repository).deleteByCreatedAtBefore(org.mockito.ArgumentMatchers.any());
  }

  /**
   * The free Render service rarely stays up for a whole interval, so the job must not wait one
   * before its first run.
   */
  @Test
  void theScheduledRunStartsAtStartUpThenRepeatsEveryInterval() throws NoSuchMethodException {
    Scheduled scheduled =
        GameCleanupService.class.getDeclaredMethod("deleteOldGames").getAnnotation(Scheduled.class);

    assertThat(scheduled.initialDelay()).isZero();
    assertThat(scheduled.initialDelayString()).isEmpty();
    assertThat(scheduled.fixedDelayString()).isEqualTo("${geogames.games.cleanup-interval:PT1H}");
  }

  @Test
  void returnsZeroWhenNothingIsOld() {
    assertThat(service.deleteGamesOlderThanMaxAge(Instant.now())).isZero();
  }
}
