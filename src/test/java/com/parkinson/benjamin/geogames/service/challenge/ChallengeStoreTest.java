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

  private static Challenge challenge(UUID id, LocalDate createdOn) {
    return new Challenge(
        id,
        "RANDOM",
        Difficulty.EASY,
        List.of(
            new PinnedRound(GameType.TRIPOINT, "tripoint:x"),
            new PinnedRound(GameType.LAKES_BY_SHAPE, "lake:Q1")),
        120,
        200,
        createdOn);
  }

  @Test
  void aStoredChallengeIsFoundByItsPublicId() {
    ChallengeStore store = store();
    Challenge stored = challenge(UUID.randomUUID(), LocalDate.of(2026, 11, 25));

    store.insert(stored);

    assertThat(store.find(stored.publicId())).contains(stored);
    assertThat(store.find(UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteRemovesOnlyChallengesCreatedBeforeTheCutoff() {
    ChallengeStore store = store();
    Challenge old = challenge(UUID.randomUUID(), LocalDate.of(2026, 11, 1));
    Challenge recent = challenge(UUID.randomUUID(), LocalDate.of(2026, 11, 20));
    store.insert(old);
    store.insert(recent);

    assertThat(store.deleteCreatedBefore(LocalDate.of(2026, 11, 10))).isEqualTo(1);

    assertThat(store.find(old.publicId())).isEmpty();
    assertThat(store.find(recent.publicId())).contains(recent);
  }
}
