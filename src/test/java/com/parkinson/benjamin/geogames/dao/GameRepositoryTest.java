package com.parkinson.benjamin.geogames.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.AnswerState;
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
        .sorted(Comparator.comparingInt(GameRoundEntity::getIndex))
        .toList();
  }

  @Test
  void persistsRoundsWithAllTheirFields() {
    GameEntity reloaded = saveAndReload(gameWithRounds("{\"first\":true}"));

    List<GameRoundEntity> rounds = sorted(reloaded);
    assertThat(rounds).hasSize(3);
    assertThat(rounds.get(0).getIndex()).isZero();
    assertThat(rounds.get(0).getGameType()).isSameAs(GameType.TRIPOINT);
    assertThat(rounds.get(0).getJsonBlob()).isEqualTo("{\"first\":true}");
    assertThat(rounds.get(0).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    assertThat(rounds.get(1).getGameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
    assertThat(rounds.get(1).getJsonBlob()).isEqualTo("{\"river\":true}");
    assertThat(rounds.get(2).getIndex()).isEqualTo(2);
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
}
