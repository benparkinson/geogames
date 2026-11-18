package com.parkinson.benjamin.geogames.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameType;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

// Use the configured H2 (PostgreSQL mode, schema from the Flyway migrations), not a fresh embedded
// one.
@DataJpaTest(properties = "spring.test.database.replace=none")
class GameRepositoryTest {

  @Autowired private GameRepository repository;

  @Autowired private TestEntityManager entityManager;

  private GameEntity gameWithRounds(String key) {
    List<GameRoundEntity> rounds = new ArrayList<>();
    rounds.add(new GameRoundEntity(0, GameType.TRIPOINT, key));
    rounds.add(new GameRoundEntity(1, GameType.RIVERS_BY_SHAPE, "river:Q1"));
    rounds.add(new GameRoundEntity(2, GameType.TRIPOINT, "tripoint:third"));
    return new GameEntity(rounds, "TRIPOINT", null);
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
  void aGameHasAPlayIdThatIsNotItsPublicId() {
    GameEntity first = saveAndReload(gameWithRounds("tripoint:a"));
    GameEntity second = saveAndReload(gameWithRounds("tripoint:b"));

    assertThat(first.getPlayId()).isNotNull().isNotEqualTo(first.getPublicId());
    assertThat(second.getPlayId()).isNotEqualTo(first.getPlayId());
  }

  @Test
  void theGameModeIsReadBack() {
    assertThat(saveAndReload(gameWithRounds("tripoint:a")).getGameMode()).isEqualTo("TRIPOINT");
  }

  @Test
  void toStringLeavesOutThePlayId() {
    GameEntity game = gameWithRounds("tripoint:a");

    assertThat(game.toString())
        .doesNotContain(game.getPlayId().toString())
        .doesNotContain(game.getPublicId().toString());
  }

  @Test
  void aNewGameHasACreationTime() {
    assertThat(gameWithRounds("tripoint:x").getCreatedAt()).isNotNull();
  }

  @Test
  void aNewGamesCreationTimeIsRoundedDownToTheHour() {
    Instant before = Instant.now();

    Instant created = gameWithRounds("tripoint:x").getCreatedAt();

    assertThat(created).isEqualTo(created.truncatedTo(ChronoUnit.HOURS));
    assertThat(created).isBeforeOrEqualTo(before);
    assertThat(created).isAfter(before.minus(Duration.ofHours(1)).minusSeconds(1));
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

    long deleted = repository.deleteCreatedBefore(now.minus(Duration.ofDays(1)));
    entityManager.flush();
    entityManager.clear();

    assertThat(deleted).isEqualTo(1);
    assertThat(repository.findAll()).extracting(GameEntity::getId).containsExactly(recentId);
    assertThat(roundCount()).isEqualTo(3);
  }

  @Test
  void deletingAGameRemovesItsRoundRows() {
    GameEntity old = gameWithRounds("tripoint:old");
    old.setCreatedAt(Instant.now().minus(Duration.ofDays(2)));
    repository.save(old);
    entityManager.flush();
    assertThat(rowsIn("game_round_entity")).isEqualTo(3);

    repository.deleteCreatedBefore(Instant.now().minus(Duration.ofDays(1)));
    entityManager.flush();
    entityManager.clear();

    assertThat(rowsIn("game_entity")).isZero();
    assertThat(rowsIn("game_round_entity")).isZero();
  }

  @Test
  void deletingWhenNothingIsOldDeletesNothing() {
    long id = repository.save(gameWithRounds("tripoint:recent")).getId();
    entityManager.flush();

    int deleted = repository.deleteCreatedBefore(Instant.now().minus(Duration.ofDays(1)));
    entityManager.clear();

    assertThat(deleted).isZero();
    assertThat(repository.findById(id)).isPresent();
  }

  @Test
  void aRoundChangedElsewhereCannotBeOverwritten() {
    GameEntity reloaded = saveAndReload(gameWithRounds("tripoint:x"));
    // Touch the rounds so they are loaded, with version 0.
    GameRoundEntity round = sorted(reloaded).get(0);

    // Another writer got there first: the row's version moves on behind Hibernate's back.
    entityManager
        .getEntityManager()
        .createNativeQuery("update game_round_entity set version = version + 1 where id = ?1")
        .setParameter(1, round.getId())
        .executeUpdate();
    round.recordAnswer(AnswerState.GAVE_UP, round.getHintsUsed(), round.getWrongGuesses());

    assertThatThrownBy(() -> entityManager.flush())
        .isInstanceOfAny(
            jakarta.persistence.OptimisticLockException.class,
            org.springframework.orm.ObjectOptimisticLockingFailureException.class);
  }

  @Test
  void answerStatesAreStoredByNameNotByNumber() {
    GameEntity game = repository.save(gameWithRounds("tripoint:x"));
    game.getRounds()
        .get(1)
        .recordAnswer(
            AnswerState.OUT_OF_GUESSES,
            game.getRounds().get(1).getHintsUsed(),
            game.getRounds().get(1).getWrongGuesses());
    entityManager.flush();

    Object stored =
        entityManager
            .getEntityManager()
            .createNativeQuery("select answer_state from game_round_entity where round_number = 1")
            .getSingleResult();

    assertThat(stored).isEqualTo("OUT_OF_GUESSES");
  }

  private long rowsIn(String table) {
    return ((Number)
            entityManager
                .getEntityManager()
                .createNativeQuery("select count(*) from " + table)
                .getSingleResult())
        .longValue();
  }

  @Test
  void persistsRoundsWithAllTheirFields() {
    GameEntity reloaded = saveAndReload(gameWithRounds("tripoint:first"));

    List<GameRoundEntity> rounds = sorted(reloaded);
    assertThat(rounds).hasSize(3);
    assertThat(rounds.get(0).getRoundNumber()).isZero();
    assertThat(rounds.get(0).getGameType()).isSameAs(GameType.TRIPOINT);
    assertThat(rounds.get(0).getRoundKey()).isEqualTo("tripoint:first");
    assertThat(rounds.get(0).getAnswerState()).isEqualTo(AnswerState.UNANSWERED);
    assertThat(rounds.get(1).getGameType()).isSameAs(GameType.RIVERS_BY_SHAPE);
    assertThat(rounds.get(1).getRoundKey()).isEqualTo("river:Q1");
    assertThat(rounds.get(2).getRoundNumber()).isEqualTo(2);
  }

  @Test
  void persistsARoundKeyAtTheColumnLimit() {
    String key = "x".repeat(255);

    GameEntity reloaded = saveAndReload(gameWithRounds(key));

    assertThat(sorted(reloaded).get(0).getRoundKey()).hasSize(255).isEqualTo(key);
  }

  @Test
  void updatingARoundAnswerThroughTheParentIsPersisted() {
    GameEntity reloaded = saveAndReload(gameWithRounds("tripoint:x"));
    sorted(reloaded)
        .get(1)
        .recordAnswer(
            AnswerState.GAVE_UP,
            sorted(reloaded).get(1).getHintsUsed(),
            sorted(reloaded).get(1).getWrongGuesses());
    sorted(reloaded)
        .get(2)
        .recordAnswer(
            AnswerState.CORRECT,
            sorted(reloaded).get(2).getHintsUsed(),
            sorted(reloaded).get(2).getWrongGuesses());

    GameEntity again = saveAndReload(reloaded);

    assertThat(sorted(again).stream().map(GameRoundEntity::getAnswerState).toList())
        .containsExactly(AnswerState.UNANSWERED, AnswerState.GAVE_UP, AnswerState.CORRECT);
  }

  @Test
  void aSavedGameHasAPublicIdThatFindsIt() {
    GameEntity saved = repository.save(gameWithRounds("tripoint:x"));
    entityManager.flush();
    entityManager.clear();

    assertThat(saved.getPublicId()).isNotNull();
    assertThat(repository.findByPublicId(saved.getPublicId()))
        .get()
        .extracting(GameEntity::getId)
        .isEqualTo(saved.getId());
    assertThat(repository.findByPublicId(UUID.randomUUID())).isEmpty();
  }

  @Test
  void twoGamesGetDifferentPublicIds() {
    GameEntity first = repository.save(gameWithRounds("tripoint:x"));
    GameEntity second = repository.save(gameWithRounds("tripoint:x"));

    assertThat(first.getPublicId()).isNotEqualTo(second.getPublicId());
  }

  @Test
  void findingAnUnknownIdGivesNothing() {
    assertThat(repository.findById(987_654L)).isEmpty();
  }

  @Test
  void everyGameTypeIsStoredAndReadBackAsTheSameConstant() {
    List<GameRoundEntity> rounds = new ArrayList<>();
    for (GameType type : GameType.values()) {
      rounds.add(new GameRoundEntity(rounds.size(), type, "tripoint:x"));
    }

    GameEntity reloaded = saveAndReload(new GameEntity(rounds, "TRIPOINT", null));

    assertThat(sorted(reloaded).stream().map(GameRoundEntity::getGameType).toList())
        .containsExactly(GameType.values());
  }

  @Test
  void gameTypesAreStoredByName() {
    GameEntity saved = repository.save(gameWithRounds("tripoint:x"));
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
    GameEntity saved = repository.save(gameWithRounds("tripoint:x"));
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
    rounds.add(new GameRoundEntity(2, GameType.TRIPOINT, "tripoint:2"));
    rounds.add(new GameRoundEntity(0, GameType.TRIPOINT, "tripoint:0"));
    rounds.add(new GameRoundEntity(1, GameType.TRIPOINT, "tripoint:1"));

    GameEntity reloaded = saveAndReload(new GameEntity(rounds, "TRIPOINT", null));

    assertThat(reloaded.getRounds().stream().map(GameRoundEntity::getRoundNumber).toList())
        .containsExactly(0, 1, 2);
  }

  @Test
  void guessesRoundTripInOrder() {
    GameEntity game = gameWithRounds("tripoint:a");
    GameRoundEntity round = game.getRounds().get(0);
    round.addGuess(new RoundGuess("Spain", false));
    round.addGuess(new RoundGuess("France", true));
    round.addGuess(new RoundGuess("Italy", false));

    GameEntity reloaded = saveAndReload(game);

    assertThat(sorted(reloaded).get(0).getGuesses())
        .extracting(RoundGuess::getGuess, RoundGuess::isCorrect)
        .containsExactly(tuple("Spain", false), tuple("France", true), tuple("Italy", false));
    assertThat(sorted(reloaded).get(1).getGuesses()).isEmpty();
  }

  @Test
  void addingAGuessBumpsTheRoundsVersion() {
    GameEntity game = saveAndReload(gameWithRounds("tripoint:a"));
    GameRoundEntity round = sorted(game).get(0);
    int before = round.getVersion();

    round.addGuess(new RoundGuess("Spain", false));
    entityManager.flush();

    assertThat(round.getVersion()).isGreaterThan(before);
  }

  @Test
  void hintsRoundTripInOrder() {
    GameEntity game = gameWithRounds("tripoint:a");
    GameRoundEntity round = game.getRounds().get(0);
    round.addHint(0);
    round.addHint(2);
    round.addHint(2);

    GameEntity reloaded = saveAndReload(game);

    assertThat(sorted(reloaded).get(0).getHintsTakenAt()).containsExactly(0, 2, 2);
    assertThat(sorted(reloaded).get(1).getHintsTakenAt()).isEmpty();
  }

  @Test
  void addingAHintBumpsTheRoundsVersion() {
    GameEntity game = saveAndReload(gameWithRounds("tripoint:a"));
    GameRoundEntity round = sorted(game).get(0);
    int before = round.getVersion();

    round.addHint(0);
    entityManager.flush();

    assertThat(round.getVersion()).isGreaterThan(before);
  }
}
