package com.parkinson.benjamin.geogames.service.stats;

import com.parkinson.benjamin.geogames.model.stats.RoundResult;
import java.sql.Types;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

/**
 * Writes {@link RoundResult}s, the flat logical row, across the normalised {@code stat_*} tables
 * that Flyway creates, in the same database as the games: one {@code stat_round} row per round,
 * one {@code stat_game} row per game, and small lookup tables for the repeated names (game mode,
 * game type with its settings, answer state, round key). The {@code round_result} view joins them
 * back to the flat shape; it is read-only.
 *
 * <p>Lookup rows are created on first use and their ids cached, so they are never deleted or
 * updated. There is deliberately no transaction: each statement commits on its own, so a cached id
 * is always committed. If the round insert fails, a lookup row or a game row with no rounds may be
 * left behind, which is harmless.
 *
 * <p>Concurrency: there is one {@code stats-} thread per instance, so with several instances a
 * get-or-create can race. The {@code unique} constraints make the loser's insert fail, and it then
 * reads the winner's row instead: never a duplicate, and no dropped row. That only works because
 * the store has no {@code @Transactional}: on Postgres a failed statement aborts the surrounding
 * transaction, so the re-select needs each statement to commit on its own.
 *
 * <p>The {@code stat_round} insert is the exception: a duplicate (same game and round number) must
 * still fail, and the recorder drops it, so the first recorded answer stays.
 */
@Component
public class RoundStatsStore {

  private record GameTypeKey(String name, int hintCount, int guessLimit) {}

  static final String ROUND_KEY_SELECT =
      "select id from stat_round_key where round_key = :roundKey";
  static final String ROUND_KEY_INSERT =
      "insert into stat_round_key (round_key) values (:roundKey)";
  static final String GAME_SELECT = "select id from stat_game where play_id = :playId";
  static final String GAME_INSERT =
      "insert into stat_game (played_on, game_mode_id, rounds_in_game, play_id, hard_mode)"
          + " values (:playedOn, :modeId, :roundsInGame, :playId, :hardMode)";

  private final JdbcClient jdbc;
  private final Map<String, Integer> modes = new ConcurrentHashMap<>();
  private final Map<GameTypeKey, Integer> gameTypes = new ConcurrentHashMap<>();
  private final Map<String, Integer> answerStates = new ConcurrentHashMap<>();
  private final Map<String, Integer> roundKeys = new ConcurrentHashMap<>();

  public RoundStatsStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(RoundResult row) {
    int modeId = modes.computeIfAbsent(row.gameMode(), this::createMode);
    int typeId =
        gameTypes.computeIfAbsent(
            new GameTypeKey(row.gameType(), row.hintCount(), row.guessLimit()), this::createType);
    int stateId = answerStates.computeIfAbsent(row.answerState(), this::createAnswerState);
    int keyId = roundKeys.computeIfAbsent(row.roundKey(), this::createRoundKey);
    int gameId = findOrCreateGame(row, modeId);

    jdbc.sql(
            """
            insert into stat_round (
              game_id, round_key_id, game_type_id, answer_state_id,
              round_number, hints_used, wrong_guesses, points)
            values (
              :gameId, :roundKeyId, :gameTypeId, :answerStateId,
              :roundNumber, :hintsUsed, :wrongGuesses, :points)
            """)
        .param("gameId", gameId)
        .param("roundKeyId", keyId)
        .param("gameTypeId", typeId)
        .param("answerStateId", stateId)
        .param("roundNumber", row.roundNumber())
        .param("hintsUsed", row.hintsUsed())
        .param("wrongGuesses", row.wrongGuesses(), java.sql.Types.SMALLINT)
        .param("points", row.points())
        .update();
  }

  private int createMode(String name) {
    return findOrCreate(
        "select id from stat_game_mode where name = :name",
        "insert into stat_game_mode (name) values (:name)",
        Map.of("name", name));
  }

  private int createType(GameTypeKey key) {
    Map<String, Object> params =
        Map.of("name", key.name(), "hintCount", key.hintCount(), "guessLimit", key.guessLimit());
    return findOrCreate(
        "select id from stat_game_type"
            + " where name = :name and hint_count = :hintCount and guess_limit = :guessLimit",
        "insert into stat_game_type (name, hint_count, guess_limit)"
            + " values (:name, :hintCount, :guessLimit)",
        params);
  }

  private int createAnswerState(String name) {
    return findOrCreate(
        "select id from stat_answer_state where name = :name",
        "insert into stat_answer_state (name) values (:name)",
        Map.of("name", name));
  }

  private int createRoundKey(String roundKey) {
    return findOrCreate(ROUND_KEY_SELECT, ROUND_KEY_INSERT, Map.of("roundKey", roundKey));
  }

  private int findOrCreateGame(RoundResult row, int modeId) {
    return findOrCreate(
        GAME_SELECT,
        GAME_INSERT,
        Map.of(
            "playedOn", row.answeredOn(),
            "modeId", modeId,
            "roundsInGame", row.roundsInGame(),
            "playId", row.playId(),
            // Map.of rejects null; SqlParameterValue is never null and types a null for Postgres.
            "hardMode", new SqlParameterValue(Types.BOOLEAN, row.hardMode())));
  }

  private int findOrCreate(String select, String insert, Map<String, Object> params) {
    return find(select, params).orElseGet(() -> insertOrFind(select, insert, params));
  }

  /**
   * Inserts the row. If another writer (the stats thread of another instance) inserted the same
   * value first, the unique constraint rejects this insert, and the winner's id is read instead.
   */
  int insertOrFind(String select, String insert, Map<String, Object> params) {
    try {
      return insertReturningId(insert, params);
    } catch (DuplicateKeyException e) {
      return find(select, params).orElseThrow(() -> e);
    }
  }

  private Optional<Integer> find(String select, Map<String, Object> params) {
    return jdbc.sql(select).params(params).query(Integer.class).optional();
  }

  private int insertReturningId(String insert, Map<String, Object> params) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.sql(insert).params(params).update(keys, "id");
    Number id = keys.getKey();
    if (id == null) {
      throw new IllegalStateException("No generated id returned for a stats insert");
    }
    return id.intValue();
  }
}
