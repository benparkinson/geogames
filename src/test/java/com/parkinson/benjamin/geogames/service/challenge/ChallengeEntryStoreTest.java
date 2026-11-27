package com.parkinson.benjamin.geogames.service.challenge;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.dao.GameEntity;
import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

// The configured H2 (PostgreSQL mode, schema from the Flyway migrations), not a fresh embedded one.
@DataJpaTest(properties = "spring.test.database.replace=none")
class ChallengeEntryStoreTest {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private GameRepository games;
  @Autowired private EntityManager entityManager;

  private ChallengeStore challenges() {
    return new ChallengeStore(JdbcClient.create(jdbcTemplate), JsonMapper.builder().build());
  }

  private ChallengeEntryStore entries() {
    return new ChallengeEntryStore(JdbcClient.create(jdbcTemplate));
  }

  private long challenge() {
    LocalDate day = LocalDate.of(2026, 11, 1);
    return challenges()
        .insert(
            new Challenge(
                UUID.randomUUID(),
                "TRIPOINT",
                Difficulty.NORMAL,
                List.of(new PinnedRound(GameType.TRIPOINT, "tripoint:x")),
                10,
                day,
                day));
  }

  @Test
  void entriesAreCountedAndTheBestScoreIsTheOneToBeat() {
    ChallengeEntryStore entries = entries();
    long first = challenge();
    long second = challenge();
    assertThat(entries.topScore(first)).isZero();
    assertThat(entries.count(first)).isZero();

    long earlier = entries.insert(first, 4);
    long later = entries.insert(first, 9);
    entries.insert(second, 10);

    assertThat(later).isGreaterThan(earlier);
    assertThat(entries.topScore(first)).isEqualTo(9);
    assertThat(entries.count(first)).isEqualTo(2);
  }

  @Test
  void theTableIsByScoreWithTheEarlierEntryWinningATie() {
    ChallengeEntryStore entries = entries();
    long challengeId = challenge();
    long four = entries.insert(challengeId, 4);
    long firstNine = entries.insert(challengeId, 9);
    long secondNine = entries.insert(challengeId, 9);
    entries.insert(challenge(), 10);

    entries.setInitials(secondNine, "AB");

    assertThat(entries.top(challengeId, 2))
        .containsExactly(
            new ChallengeEntryStore.Entry(firstNine, 9, null),
            new ChallengeEntryStore.Entry(secondNine, 9, "AB"));
    assertThat(entries.rank(firstNine)).isEqualTo(1);
    assertThat(entries.rank(secondNine)).isEqualTo(2);
    assertThat(entries.rank(four)).isEqualTo(3);
    assertThat(entries.find(four)).contains(new ChallengeEntryStore.Entry(four, 4, null));
  }

  @Test
  void deletingAChallengeDeletesItsEntriesAndUnlinksItsGames() {
    long challengeId = challenge();
    long entryId = entries().insert(challengeId, 4);
    GameEntity game =
        new GameEntity(
            new ArrayList<>(List.of(new GameRoundEntity(0, GameType.TRIPOINT, "tripoint:x"))),
            "TRIPOINT",
            Difficulty.NORMAL);
    game.linkChallenge(challengeId, entryId);
    long gameId = games.save(game).getId();
    entityManager.flush();
    entityManager.clear();

    challenges().deleteUnusedBefore(LocalDate.of(2026, 11, 2));

    assertThat(entries().count(challengeId)).isZero();
    GameEntity unlinked = games.findById(gameId).orElseThrow();
    assertThat(unlinked.getChallengeId()).isNull();
    assertThat(unlinked.getChallengeEntryId()).isNull();
  }
}
