package com.parkinson.benjamin.geogames.service.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.model.stats.RoundResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

// The full context, because it runs the Flyway migrations on the in-memory H2 that the tests use.
// Lookup tables are never cleaned: the store caches their ids and the H2 database is shared by
// every cached Spring context, so lookup rows are counted by name.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RoundStatsStoreTest {

  @Autowired private RoundStatsStore store;
  @Autowired private JdbcClient jdbc;

  @AfterEach
  void cleanTables() {
    jdbc.sql("delete from stat_round").update();
    jdbc.sql("delete from stat_game").update();
  }

  private static RoundResult row(UUID playId, int roundNumber, String roundKey, Integer wrong) {
    return row(playId, roundNumber, roundKey, wrong, 3);
  }

  private static RoundResult row(
      UUID playId, int roundNumber, String roundKey, Integer wrong, int hintCount) {
    return new RoundResult(
        LocalDate.of(2026, 11, 9),
        playId,
        "RANDOM",
        5,
        roundNumber,
        "RIVERS_BY_SHAPE",
        roundKey,
        "CORRECT",
        1,
        wrong,
        hintCount,
        5,
        83);
  }

  private static RoundResult row(String roundKey, Integer wrongGuesses) {
    return row(UUID.randomUUID(), 3, roundKey, wrongGuesses);
  }

  private List<Map<String, Object>> rows() {
    return jdbc.sql("select * from round_result order by play_id, round_number")
        .query()
        .listOfRows();
  }

  private int count(String sql, Object param) {
    return jdbc.sql(sql).param(param).query(Integer.class).single();
  }

  @Test
  void anInsertIsReadBackWithEveryColumn() {
    RoundResult result = row("river:Q3392", 2);

    store.insert(result);

    assertThat(rows()).hasSize(1);
    Map<String, Object> read = rows().get(0);
    assertThat(read.get("answered_on")).hasToString("2026-11-09");
    assertThat(read.get("play_id")).isEqualTo(result.playId());
    assertThat(read)
        .containsEntry("game_mode", "RANDOM")
        .containsEntry("game_type", "RIVERS_BY_SHAPE")
        .containsEntry("round_key", "river:Q3392")
        .containsEntry("answer_state", "CORRECT");
    assertThat(((Number) read.get("rounds_in_game")).intValue()).isEqualTo(5);
    assertThat(((Number) read.get("round_number")).intValue()).isEqualTo(3);
    assertThat(((Number) read.get("hints_used")).intValue()).isEqualTo(1);
    assertThat(((Number) read.get("wrong_guesses")).intValue()).isEqualTo(2);
    assertThat(((Number) read.get("hint_count")).intValue()).isEqualTo(3);
    assertThat(((Number) read.get("guess_limit")).intValue()).isEqualTo(5);
    assertThat(((Number) read.get("points")).intValue()).isEqualTo(83);
  }

  @Test
  void aNullWrongGuessesIsReadBackAsNullNotZero() {
    store.insert(row("country:France", null));

    assertThat(rows().get(0).get("wrong_guesses")).isNull();
  }

  @Test
  void twoRoundsOfOneGameMakeOneGameRow() {
    UUID play = UUID.randomUUID();
    store.insert(row(play, 0, "country:France", 0));
    store.insert(row(play, 1, "country:Spain", 1));

    assertThat(count("select count(*) from stat_game where play_id = ?", play)).isEqualTo(1);
    assertThat(
            count(
                "select count(*) from stat_round r join stat_game g on g.id = r.game_id"
                    + " where g.play_id = ?",
                play))
        .isEqualTo(2);
    assertThat(rows())
        .extracting(r -> r.get("round_key"))
        .containsExactly("country:France", "country:Spain");
  }

  @Test
  void lookupRowsAreSharedBetweenGames() {
    store.insert(row(UUID.randomUUID(), 0, "river:Q-shared", 0));
    store.insert(row(UUID.randomUUID(), 0, "river:Q-shared", 0));

    assertThat(count("select count(*) from stat_round_key where round_key = ?", "river:Q-shared"))
        .isEqualTo(1);
    assertThat(
            count(
                "select count(*) from stat_game_type where name = ? and hint_count = 3",
                "RIVERS_BY_SHAPE"))
        .isEqualTo(1);
  }

  @Test
  void aChangedHintCountMakesANewGameTypeRow() {
    store.insert(row(UUID.randomUUID(), 0, "river:Q1", 0, 7));
    store.insert(row(UUID.randomUUID(), 0, "river:Q2", 0, 8));

    assertThat(
            count(
                "select count(*) from stat_game_type where name = ? and hint_count in (7, 8)",
                "RIVERS_BY_SHAPE"))
        .isEqualTo(2);
    assertThat(rows())
        .extracting(r -> ((Number) r.get("hint_count")).intValue())
        .containsExactlyInAnyOrder(7, 8);
  }

  @Test
  void theSameRoundTwiceIsRejectedAndLeavesOneRow() {
    UUID play = UUID.randomUUID();
    store.insert(row(play, 0, "country:France", 0));

    assertThatThrownBy(() -> store.insert(row(play, 0, "country:France", 0)))
        .isInstanceOf(Exception.class);
    assertThat(rows()).hasSize(1);
  }

  @Test
  void playedOnIsADateWithNoTimeOfDay() {
    store.insert(row("country:France", 0));

    String type =
        jdbc.sql(
                "select data_type from information_schema.columns"
                    + " where lower(table_name) = 'stat_game' and lower(column_name) = 'played_on'")
            .query(String.class)
            .single();
    assertThat(type).isEqualToIgnoringCase("DATE");
    assertThat(rows().get(0).get("answered_on")).hasToString("2026-11-09");
  }
}
