package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

/** Plays through the API and checks the anonymous rows that end up in {@code round_result}. */
@SpringBootTest
@AutoConfigureMockMvc
class RoundStatsIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcClient jdbc;

  private String createGame(String type, int rounds) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/games")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"gameType\":\"%s\",\"numberOfRounds\":%d}".formatted(type, rounds)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private void answer(String gameId, int round, String json) throws Exception {
    mockMvc
        .perform(
            post("/api/games/%s/rounds/%d/answers".formatted(gameId, round))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk());
  }

  /** The rows of one game, found through its play id, which only this test reads from the games. */
  private List<Map<String, Object>> rowsOf(String gameId) {
    UUID playId =
        jdbc.sql("select play_id from game_entity where public_id = :id")
            .param("id", UUID.fromString(gameId))
            .query(UUID.class)
            .single();
    return jdbc.sql("select * from round_result where play_id = :p order by round_number")
        .param("p", playId)
        .query()
        .listOfRows();
  }

  private String roundKey(String gameId, int round) {
    return jdbc.sql(
            """
            select r.round_key from game_round_entity r join game_entity g on g.id = r.game_id
            where g.public_id = :id and r.round_number = :n
            """)
        .param("id", UUID.fromString(gameId))
        .param("n", round)
        .query(String.class)
        .single();
  }

  @Test
  void answeredRoundsAreRecordedOnceAndAnonymously() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 2);

    answer(id, 0, "{\"answerState\":\"CORRECT\",\"hintsUsed\":1,\"wrongGuesses\":2}");
    await().atMost(Duration.ofSeconds(10)).until(() -> rowsOf(id).size() == 1);

    Map<String, Object> row = rowsOf(id).get(0);
    assertThat(row)
        .containsEntry("game_type", "RIVERS_BY_SHAPE")
        .containsEntry("game_mode", "RIVERS_BY_SHAPE")
        .containsEntry("round_key", roundKey(id, 0))
        .containsEntry("answer_state", "CORRECT");
    assertThat(((Number) row.get("round_number")).intValue()).isZero();
    assertThat(((Number) row.get("rounds_in_game")).intValue()).isEqualTo(2);
    assertThat(((Number) row.get("hints_used")).intValue()).isEqualTo(1);
    assertThat(((Number) row.get("wrong_guesses")).intValue()).isEqualTo(2);

    // An old client doesn't send wrongGuesses: that is recorded as unknown, not 0.
    answer(id, 1, "{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0}");
    await().atMost(Duration.ofSeconds(10)).until(() -> rowsOf(id).size() == 2);
    assertThat(rowsOf(id).get(1).get("wrong_guesses")).isNull();

    // Repeating an answer is a no-op, so it records nothing. The one stats thread runs in order, so
    // once a later answer's row has arrived, the repeat has been handled too.
    answer(id, 0, "{\"answerState\":\"CORRECT\",\"hintsUsed\":1,\"wrongGuesses\":2}");
    String other = createGame("RIVERS_BY_SHAPE", 1);
    answer(other, 0, "{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0}");
    await().atMost(Duration.ofSeconds(10)).until(() -> rowsOf(other).size() == 1);
    assertThat(rowsOf(id)).hasSize(2);

    // No column of any row holds a game's public id.
    List<Map<String, Object>> all = jdbc.sql("select * from round_result").query().listOfRows();
    assertThat(all).isNotEmpty();
    assertThat(all.toString()).doesNotContain(id).doesNotContain(other);
    assertThat(rowsOf(id).get(0).get("play_id")).isNotEqualTo(UUID.fromString(id));
  }
}
