package com.parkinson.benjamin.geogames.service.stats;

import com.parkinson.benjamin.geogames.model.stats.RoundResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Writes {@link RoundResult}s to the {@code round_result} table, which Flyway creates, in the same
 * database as the games. Plain JDBC: it is one insert-only table.
 */
@Component
public class RoundStatsStore {

  private static final String INSERT =
      """
      insert into round_result (
        answered_on, play_id, game_mode, rounds_in_game, round_number, game_type, round_key,
        answer_state, hints_used, wrong_guesses, hint_count, guess_limit, points)
      values (
        :answeredOn, :playId, :gameMode, :roundsInGame, :roundNumber, :gameType, :roundKey,
        :answerState, :hintsUsed, :wrongGuesses, :hintCount, :guessLimit, :points)
      """;

  private final JdbcClient jdbc;

  public RoundStatsStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(RoundResult row) {
    jdbc.sql(INSERT)
        .param("answeredOn", row.answeredOn())
        .param("playId", row.playId())
        .param("gameMode", row.gameMode())
        .param("roundsInGame", row.roundsInGame())
        .param("roundNumber", row.roundNumber())
        .param("gameType", row.gameType())
        .param("roundKey", row.roundKey())
        .param("answerState", row.answerState())
        .param("hintsUsed", row.hintsUsed())
        .param("wrongGuesses", row.wrongGuesses(), java.sql.Types.SMALLINT)
        .param("hintCount", row.hintCount())
        .param("guessLimit", row.guessLimit())
        .param("points", row.points())
        .update();
  }
}
