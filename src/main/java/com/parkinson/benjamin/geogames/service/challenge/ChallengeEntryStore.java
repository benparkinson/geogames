package com.parkinson.benjamin.geogames.service.challenge;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
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

  /** One entry; {@code initials} is null until entered. */
  public record Entry(long id, int score, String initials) {}

  /** The best entries in rank order: by score, and the earlier entry wins a tie. */
  public List<Entry> top(long challengeId, int limit) {
    return jdbc.sql(
            "select id, score, initials from challenge_entry where challenge_id = :challenge"
                + " order by score desc, id limit :limit")
        .param("challenge", challengeId)
        .param("limit", limit)
        .query(ChallengeEntryStore::entry)
        .list();
  }

  public Optional<Entry> find(long entryId) {
    return jdbc.sql("select id, score, initials from challenge_entry where id = :id")
        .param("id", entryId)
        .query(ChallengeEntryStore::entry)
        .optional();
  }

  /** 1 plus the number of entries that beat this one. The entry must exist. */
  public int rank(long entryId) {
    return jdbc.sql(
            "select count(other.id) + 1 from challenge_entry me left join challenge_entry other"
                + " on other.challenge_id = me.challenge_id and (other.score > me.score"
                + " or (other.score = me.score and other.id < me.id)) where me.id = :id")
        .param("id", entryId)
        .query(Integer.class)
        .single();
  }

  /**
   * Sets the initials only if the entry still holds {@code expected} (which may be null). Returns
   * whether it did.
   */
  public boolean setInitials(long entryId, String expected, String initials) {
    return jdbc.sql(
                "update challenge_entry set initials = :initials where id = :id"
                    + " and initials is not distinct from :expected")
            .param("initials", initials)
            .param("id", entryId)
            .param("expected", expected, Types.VARCHAR)
            .update()
        == 1;
  }

  private static Entry entry(ResultSet rs, int row) throws SQLException {
    return new Entry(rs.getLong("id"), rs.getInt("score"), rs.getString("initials"));
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
