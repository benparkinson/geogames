package com.parkinson.benjamin.geogames.service.challenge;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
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

  public void insert(Challenge challenge) {
    jdbc.sql(
            "insert into challenge (public_id, game_mode, difficulty, rounds, score, max_score,"
                + " created_on) values (:id, :mode, :difficulty, :rounds, :score, :max, :on)")
        .param("id", challenge.publicId())
        .param("mode", challenge.gameMode())
        .param("difficulty", challenge.difficulty().name())
        .param("rounds", objectMapper.writeValueAsString(challenge.rounds()))
        .param("score", challenge.score())
        .param("max", challenge.maxScore())
        .param("on", challenge.createdOn())
        .update();
  }

  public Optional<Challenge> find(UUID publicId) {
    return jdbc.sql(
            "select public_id, game_mode, difficulty, rounds, score, max_score, created_on"
                + " from challenge where public_id = :id")
        .param("id", publicId)
        .query(
            (rs, row) ->
                new Challenge(
                    rs.getObject("public_id", UUID.class),
                    rs.getString("game_mode"),
                    Difficulty.valueOf(rs.getString("difficulty")),
                    objectMapper.readValue(rs.getString("rounds"), PinnedRound.LIST),
                    rs.getInt("score"),
                    rs.getInt("max_score"),
                    rs.getObject("created_on", LocalDate.class)))
        .optional();
  }

  /** Deletes challenges created before {@code cutoff}; deleting none is not an error. */
  public int deleteCreatedBefore(LocalDate cutoff) {
    return jdbc.sql("delete from challenge where created_on < :cutoff")
        .param("cutoff", cutoff)
        .update();
  }
}
