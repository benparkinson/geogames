package com.parkinson.benjamin.geogames.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class GameRepositoryTest {

  @Autowired private GameRepository repository;

  @Autowired private TestEntityManager entityManager;

  private GameEntity gameWithRounds(String blob) {
    List<GameRoundEntity> rounds = new ArrayList<>();
    rounds.add(new GameRoundEntity(0, GameType.TRIPOINT, blob));
    rounds.add(new GameRoundEntity(1, GameType.RIVERS_BY_SHAPE, "{\"river\":true}"));
    rounds.add(new GameRoundEntity(2, GameType.TRIPOINT, "{\"third\":true}"));
    return new GameEntity(rounds);
  }

  private GameEntity saveAndReload(GameEntity game) {
    GameEntity saved = repository.save(game);
    entityManager.flush();
    entityManager.clear();
    return repository.findById(saved.getId()).orElseThrow();
  }

  private List<GameRoundEntity> sorted(GameEntity game) {
    return game.getRounds().stream()
        .sorted(Comparator.comparingInt(GameRoundEntity::getRoundNumber))
        .toList();
  }

  private long roundCount() {
    return entityManager
        .getEntityManager()
        .createQuery("select count(r) from GameRoundEntity r", Long.class)
        .getSingleResult();
  }

  @Test
  void aNewGameHasACreationTime() {
    assertThat(gameWithRounds("{}").getCreatedAt()).isNotNull();
  }

  @Test
  void deletingOldGamesRemovesTheirRoundsToo() {
    Instant now = Instant.now();
    GameEntity old = gameWithRounds("{\"old\":true}");
    old.setCreatedAt(now.minus(Duration.ofDays(2)));
    GameEntity recent = gameWithRounds("{\"new\":true}");
    long recentId = repository.save(recent).getId();
    repository.save(old);
    entityManager.flush();

    long deleted = repository.deleteByCreatedAtBefore(now.minus(Duration.ofDays(1)));
    entityManager.flush();
    entityManager.clear();

    assertThat(deleted).isEqualTo(1);
    assertThat(repository.findAll()).extracting(GameEntity::getId).containsExactly(recentId);
    assertThat(roundCount()).isEqualTo(3);
  }

  @Test
  void persistsRoundsWithAllTheirFields() {
    GameEntity reloaded = saveAndReload(gameWithRounds("{\"first\":true}"));

    List<GameRoundEntity> rounds = sorted(reloaded);
    assertThat(rounds).hasSize(3);
    assertThat(rounds.get(0).getRoundNumber()).isZero();
    assertThat(rounds.get(0).getGameType()).isSameAs(GameType.TRIPOINT);
    assertThat(rounds.get(0).getJsonBlob()).isEqualTo("{\"first\":true}");
    assertThat(rounds.get(0).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    assertThat(rounds.get(1).getGameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
    assertThat(rounds.get(1).getJsonBlob()).isEqualTo("{\"river\":true}");
    assertThat(rounds.get(2).getRoundNumber()).isEqualTo(2);
  }

  @Test
  void persistsALargeJsonBlob() {
    String blob = "x".repeat(2_000_000);

    GameEntity reloaded = saveAndReload(gameWithRounds(blob));

    assertThat(sorted(reloaded).get(0).getJsonBlob()).hasSize(2_000_000).isEqualTo(blob);
  }

  @Test
  void updatingARoundAnswerThroughTheParentIsPersisted() {
    GameEntity reloaded = saveAndReload(gameWithRounds("{}"));
    sorted(reloaded).get(1).setAnswerState(AnswerState.GAVE_UP);
    sorted(reloaded).get(2).setAnswerState(AnswerState.CORRECT);

    GameEntity again = saveAndReload(reloaded);

    assertThat(sorted(again).stream().map(GameRoundEntity::getAnswerState).toList())
        .containsExactly(AnswerState.UNANSWERED, AnswerState.GAVE_UP, AnswerState.CORRECT);
  }

  @Test
  void findingAnUnknownIdGivesNothing() {
    assertThat(repository.findById(987_654L)).isEmpty();
  }

  @Test
  void everyGameTypeIsStoredAndReadBackAsTheSameConstant() {
    List<GameRoundEntity> rounds = new ArrayList<>();
    for (GameType type : GameType.values()) {
      rounds.add(new GameRoundEntity(rounds.size(), type, "{}"));
    }

    GameEntity reloaded = saveAndReload(new GameEntity(rounds));

    assertThat(sorted(reloaded).stream().map(GameRoundEntity::getGameType).toList())
        .containsExactly(GameType.values());
  }

  @Test
  void gameTypesAreStoredByName() {
    GameEntity saved = repository.save(gameWithRounds("{}"));
    entityManager.flush();

    Object stored =
        entityManager
            .getEntityManager()
            .createNativeQuery("select min(game_type) from game_round_entity")
            .getSingleResult();

    assertThat(stored).isEqualTo("RIVERS_BY_SHAPE");
    assertThat(saved.getId()).isPositive();
  }

  @Test
  void anUnknownStoredGameTypeFailsInsteadOfBecomingAnotherType() {
    GameEntity saved = repository.save(gameWithRounds("{}"));
    entityManager.flush();
    entityManager.clear();

    // Either the database refuses the bad name or loading the round does. It is never mapped to
    // some other game type.
    assertThatThrownBy(
            () -> {
              entityManager
                  .getEntityManager()
                  .createNativeQuery("update game_round_entity set game_type = 'SOMETHING_ELSE'")
                  .executeUpdate();
              entityManager.clear();
              repository.findById(saved.getId()).orElseThrow().getRounds().size();
            })
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void roundsComeBackInRoundNumberOrderWhateverOrderTheyWereSavedIn() {
    List<GameRoundEntity> rounds = new ArrayList<>();
    rounds.add(new GameRoundEntity(2, GameType.TRIPOINT, "{\"n\":2}"));
    rounds.add(new GameRoundEntity(0, GameType.TRIPOINT, "{\"n\":0}"));
    rounds.add(new GameRoundEntity(1, GameType.TRIPOINT, "{\"n\":1}"));

    GameEntity reloaded = saveAndReload(new GameEntity(rounds));

    assertThat(reloaded.getRounds().stream().map(GameRoundEntity::getRoundNumber).toList())
        .containsExactly(0, 1, 2);
  }
}
