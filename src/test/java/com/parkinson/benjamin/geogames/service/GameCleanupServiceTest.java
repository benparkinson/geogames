package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parkinson.benjamin.geogames.dao.GameRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

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

  @Test
  void returnsZeroWhenNothingIsOld() {
    assertThat(service.deleteGamesOlderThanMaxAge(Instant.now())).isZero();
  }
}
