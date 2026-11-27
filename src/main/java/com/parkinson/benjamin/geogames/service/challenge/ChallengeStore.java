package com.parkinson.benjamin.geogames.service.challenge;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * The {@code challenge} table. Deliberately not {@code @Transactional}: each call is one
 * statement.
 */
@Component
public class ChallengeStore {

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public ChallengeStore(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  /** Returns the new row's primary key. */
  public long insert(Challenge challenge) {
    GeneratedKeyHolder key = new GeneratedKeyHolder();
    jdbc.sql(
            "insert into challenge (public_id, game_mode, difficulty, rounds, max_score,"
                + " created_on, last_used_on)"
                + " values (:id, :mode, :difficulty, :rounds, :max, :on, :used)")
        .param("id", challenge.publicId())
        .param("mode", challenge.gameMode())
        .param("difficulty", challenge.difficulty().name())
        .param("rounds", objectMapper.writeValueAsString(challenge.rounds()))
        .param("max", challenge.maxScore())
        .param("on", challenge.createdOn())
        .param("used", challenge.lastUsedOn())
        .update(key, "id");
    return key.getKey().longValue();
  }

  public Optional<StoredChallenge> find(UUID publicId) {
    return findWhere("public_id", publicId);
  }

  public Optional<StoredChallenge> findById(long id) {
    return findWhere("id", id);
  }

  private Optional<StoredChallenge> findWhere(String column, Object value) {
    return jdbc.sql(
            "select id, public_id, game_mode, difficulty, rounds, max_score, created_on,"
                + " last_used_on from challenge where "
                + column
                + " = :value")
        .param("value", value)
        .query(
            (rs, row) ->
                new StoredChallenge(
                    rs.getLong("id"),
                    new Challenge(
                        rs.getObject("public_id", UUID.class),
                        rs.getString("game_mode"),
                        Difficulty.valueOf(rs.getString("difficulty")),
                        objectMapper.readValue(rs.getString("rounds"), PinnedRound.LIST),
                        rs.getInt("max_score"),
                        rs.getObject("created_on", LocalDate.class),
                        rs.getObject("last_used_on", LocalDate.class))))
        .optional();
  }

  /** Records that the challenge was used on {@code today}; the date only moves forward. */
  public void touch(long id, LocalDate today) {
    jdbc.sql("update challenge set last_used_on = :today where id = :id and last_used_on < :today")
        .param("today", today)
        .param("id", id)
        .update();
  }

  /**
   * Deletes challenges last used before {@code cutoff}, with their entries; deleting none is not
   * an error.
   */
  public int deleteUnusedBefore(LocalDate cutoff) {
    return jdbc.sql("delete from challenge where last_used_on < :cutoff")
        .param("cutoff", cutoff)
        .update();
  }
}
