package com.parkinson.benjamin.geogames.service.challenge;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import java.time.LocalDate;
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
class ChallengeStoreTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  private ChallengeStore store() {
    return new ChallengeStore(JdbcClient.create(jdbcTemplate), JsonMapper.builder().build());
  }

  private static Challenge challenge(LocalDate lastUsedOn) {
    return new Challenge(
        UUID.randomUUID(),
        "RANDOM",
        Difficulty.EASY,
        List.of(
            new PinnedRound(GameType.TRIPOINT, "tripoint:x"),
            new PinnedRound(GameType.LAKES_BY_SHAPE, "lake:Q1")),
        200,
        LocalDate.of(2026, 11, 1),
        lastUsedOn);
  }

  @Test
  void aStoredChallengeIsFoundByEitherId() {
    ChallengeStore store = store();
    Challenge challenge = challenge(LocalDate.of(2026, 11, 25));

    long id = store.insert(challenge);

    assertThat(store.find(challenge.publicId())).contains(new StoredChallenge(id, challenge));
    assertThat(store.findById(id)).contains(new StoredChallenge(id, challenge));
    assertThat(store.find(UUID.randomUUID())).isEmpty();
    assertThat(store.findById(id + 1000)).isEmpty();
  }

  @Test
  void touchOnlyMovesTheLastUsedDateForward() {
    ChallengeStore store = store();
    long id = store.insert(challenge(LocalDate.of(2026, 11, 20)));

    store.touch(id, LocalDate.of(2026, 11, 10));
    assertThat(store.findById(id).orElseThrow().challenge().lastUsedOn())
        .isEqualTo(LocalDate.of(2026, 11, 20));

    store.touch(id, LocalDate.of(2026, 11, 22));
    assertThat(store.findById(id).orElseThrow().challenge().lastUsedOn())
        .isEqualTo(LocalDate.of(2026, 11, 22));
  }

  @Test
  void deleteGoesByWhenAChallengeWasLastUsed() {
    ChallengeStore store = store();
    long unused = store.insert(challenge(LocalDate.of(2026, 11, 1)));
    long touched = store.insert(challenge(LocalDate.of(2026, 11, 1)));
    store.touch(touched, LocalDate.of(2026, 11, 20));

    store.deleteUnusedBefore(LocalDate.of(2026, 11, 10));

    assertThat(store.findById(unused)).isEmpty();
    assertThat(store.findById(touched)).isPresent();
  }
}
