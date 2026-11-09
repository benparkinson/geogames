package com.parkinson.benjamin.geogames.service.stats;

import com.parkinson.benjamin.geogames.model.stats.RoundResult;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
 * <p>Concurrency: this is only called from the one {@code stats-} thread, so a get-or-create cannot
 * race. If it ever did, the {@code unique} constraints would reject the second insert, the recorder
 * would log its WARN and that one row would be dropped: never a duplicate.
 */
@Component
public class RoundStatsStore {

  private record GameTypeKey(String name, int hintCount, int guessLimit) {}

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
    return findOrCreate(
        "select id from stat_round_key where round_key = :roundKey",
        "insert into stat_round_key (round_key) values (:roundKey)",
        Map.of("roundKey", roundKey));
  }

  private int findOrCreateGame(RoundResult row, int modeId) {
    Optional<Integer> existing =
        jdbc.sql("select id from stat_game where play_id = :playId")
            .param("playId", row.playId())
            .query(Integer.class)
            .optional();
    if (existing.isPresent()) {
      return existing.get();
    }
    return insertReturningId(
        "insert into stat_game (played_on, game_mode_id, rounds_in_game, play_id)"
            + " values (:playedOn, :modeId, :roundsInGame, :playId)",
        Map.of(
            "playedOn", row.answeredOn(),
            "modeId", modeId,
            "roundsInGame", row.roundsInGame(),
            "playId", row.playId()));
  }

  private int findOrCreate(String select, String insert, Map<String, Object> params) {
    Optional<Integer> existing = jdbc.sql(select).params(params).query(Integer.class).optional();
    return existing.orElseGet(() -> insertReturningId(insert, params));
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
