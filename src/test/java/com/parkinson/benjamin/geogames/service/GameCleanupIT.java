package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.model.GameType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Runs the scheduled cleanup the way the scheduler does: from the real bean, with no surrounding
 * transaction. {@code GameCleanupServiceTest} mocks the repository, and a {@code @DataJpaTest}
 * wraps each test in a transaction, which hides a missing one.
 *
 * <p>Deliberately not {@code @Transactional}.
 */
@SpringBootTest
class GameCleanupIT {

  @Autowired private GameCleanupService service;
  @Autowired private GameRepository games;

  private GameEntity game(String key) {
    List<GameRoundEntity> rounds = new ArrayList<>();
    rounds.add(new GameRoundEntity(0, GameType.TRIPOINT, key));
    return new GameEntity(rounds, "TRIPOINT");
  }

  @Test
  void theScheduledRunDeletesOldGamesAndKeepsNewOnes() {
    GameEntity old = game("tripoint:cleanup-old");
    old.setCreatedAt(Instant.now().minus(Duration.ofDays(2)));
    GameEntity recent = game("tripoint:cleanup-recent");
    games.save(old);
    games.save(recent);

    service.deleteOldGames();

    assertThat(games.findByPublicId(old.getPublicId())).isEmpty();
    assertThat(games.findByPublicId(recent.getPublicId())).isPresent();
  }
}
