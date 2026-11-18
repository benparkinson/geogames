package com.parkinson.benjamin.geogames.service.daily;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * The rounds of each day's daily challenge, pinned the first time anyone asks for that date. One
 * row per date, so writing it is atomic: another instance never sees half a day.
 *
 * <p>Deliberately not {@code @Transactional}: on Postgres a failed insert aborts the transaction
 * it is in, and the loser of a race must go on to read the winner's row.
 */
@Component
public class DailyChallengeStore {

  private static final TypeReference<List<PinnedRound>> ROUNDS = new TypeReference<>() {};

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public DailyChallengeStore(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  public Optional<List<PinnedRound>> find(LocalDate date) {
    return jdbc.sql("select rounds from daily_challenge where challenge_date = :date")
        .param("date", date)
        .query(String.class)
        .optional()
        .map(json -> objectMapper.readValue(json, ROUNDS));
  }

  /**
   * Returns the stored rounds for {@code date}, generating and storing them first if there are
   * none. If another instance stored them first, returns theirs.
   */
  public List<PinnedRound> findOrCreate(LocalDate date, Supplier<List<PinnedRound>> generate) {
    Optional<List<PinnedRound>> existing = find(date);
    if (existing.isPresent()) {
      return existing.get();
    }
    String json = objectMapper.writeValueAsString(generate.get());
    try {
      jdbc.sql("insert into daily_challenge (challenge_date, rounds) values (:date, :rounds)")
          .param("date", date)
          .param("rounds", json)
          .update();
    } catch (DuplicateKeyException lostTheRace) {
      // Another instance stored the day first: read its rows below.
    }
    return find(date).orElseThrow(() -> new IllegalStateException("Daily rounds were not stored"));
  }
}
