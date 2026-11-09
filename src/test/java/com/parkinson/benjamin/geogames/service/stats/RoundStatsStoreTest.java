package com.parkinson.benjamin.geogames.service.stats;

import static org.assertj.core.api.Assertions.assertThat;

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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RoundStatsStoreTest {

  @Autowired private RoundStatsStore store;
  @Autowired private JdbcClient jdbc;

  @AfterEach
  void cleanTable() {
    jdbc.sql("delete from round_result").update();
  }

  private static RoundResult row(String roundKey, Integer wrongGuesses) {
    return new RoundResult(
        LocalDate.of(2026, 11, 9),
        UUID.randomUUID(),
        "RANDOM",
        5,
        3,
        "RIVERS_BY_SHAPE",
        roundKey,
        "CORRECT",
        1,
        wrongGuesses,
        3,
        5,
        83);
  }

  private List<Map<String, Object>> rows() {
    return jdbc.sql("select * from round_result order by id").query().listOfRows();
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
  void twoInsertsBothWork() {
    store.insert(row("country:France", 0));
    store.insert(row("country:Spain", 1));

    assertThat(rows())
        .extracting(r -> r.get("round_key"))
        .containsExactly("country:France", "country:Spain");
  }
}
