package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.dao.RoundGuess;
import com.parkinson.benjamin.geogames.model.GameType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

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
  @Autowired private JdbcTemplate jdbc;

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

  @Test
  void deletingAGameDeletesItsGuessRows() {
    GameEntity old = game("tripoint:cleanup-guesses");
    old.getRounds().get(0).addGuess(new RoundGuess("France", false));
    old.getRounds().get(0).addGuess(new RoundGuess("Spain", true));
    old.setCreatedAt(Instant.now().minus(Duration.ofDays(2)));
    GameEntity saved = games.save(old);
    long roundId = saved.getRounds().get(0).getId();
    assertThat(guessRows(roundId)).isEqualTo(2);

    service.deleteOldGames();

    assertThat(games.findByPublicId(saved.getPublicId())).isEmpty();
    assertThat(guessRows(roundId)).isZero();
  }

  private long guessRows(long roundId) {
    return jdbc.queryForObject(
        "select count(*) from game_round_guess where round_id = ?", Long.class, roundId);
  }

  @Test
  void deletingAGameDeletesItsHintRows() {
    GameEntity old = game("tripoint:cleanup-hints");
    old.getRounds().get(0).addHint(0);
    old.getRounds().get(0).addHint(1);
    old.setCreatedAt(Instant.now().minus(Duration.ofDays(2)));
    GameEntity saved = games.save(old);
    long roundId = saved.getRounds().get(0).getId();
    assertThat(hintRows(roundId)).isEqualTo(2);

    service.deleteOldGames();

    assertThat(hintRows(roundId)).isZero();
  }

  private long hintRows(long roundId) {
    return jdbc.queryForObject(
        "select count(*) from game_round_hint where round_id = ?", Long.class, roundId);
  }
}
