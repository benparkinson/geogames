package com.parkinson.benjamin.geogames.service.challenge;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

/**
 * The {@code challenge_entry} table: one score per finished game of a challenge. Not {@code
 * @Transactional}: each call is one statement, and an insert joins the caller's transaction.
 */
@Component
public class ChallengeEntryStore {

  private final JdbcClient jdbc;

  public ChallengeEntryStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Returns the new entry's id. */
  public long insert(long challengeId, int score) {
    GeneratedKeyHolder key = new GeneratedKeyHolder();
    jdbc.sql("insert into challenge_entry (challenge_id, score) values (:challenge, :score)")
        .param("challenge", challengeId)
        .param("score", score)
        .update(key, "id");
    return key.getKey().longValue();
  }

  /** The score to beat: the best entry's, or 0 with no entries. */
  public int topScore(long challengeId) {
    return jdbc.sql(
            "select coalesce(max(score), 0) from challenge_entry where challenge_id = :challenge")
        .param("challenge", challengeId)
        .query(Integer.class)
        .single();
  }

  public int count(long challengeId) {
    return jdbc.sql("select count(*) from challenge_entry where challenge_id = :challenge")
        .param("challenge", challengeId)
        .query(Integer.class)
        .single();
  }
}
