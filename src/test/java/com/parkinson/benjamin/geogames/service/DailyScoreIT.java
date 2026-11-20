package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.service.daily.DailyScoreStore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The daily totals through the real API and database. Deliberately not {@code @Transactional}: the
 * count is made inside the answer's own transaction, and a test transaction would hide a rollback.
 * The daily date is today (UTC), the only one a game can be made for besides its neighbours, so
 * the row is cleared before and after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DailyScoreIT {

  private static final LocalDate DATE = LocalDate.now(java.time.ZoneOffset.UTC);

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private GameCleanupService cleanup;

  @MockitoSpyBean private DailyScoreStore store;

  @BeforeEach
  @AfterEach
  void clearTheDay() {
    org.mockito.Mockito.reset(store);
    jdbc.update("delete from daily_score_by_difficulty where challenge_date = ?", DATE);
  }

  private String createDaily() throws Exception {
    return createDaily("");
  }

  /** @param extra more JSON members for the request, such as {@code ,"difficulty":"EASY"} */
  private String createDaily(String extra) throws Exception {
    return objectMapper
        .readTree(
            mockMvc
                .perform(
                    post("/api/daily-games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"%s\"%s}".formatted(DATE, extra)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .get("id")
        .asString();
  }

  private void answer(String id, int round, AnswerState state) throws Exception {
    mockMvc
        .perform(
            post("/api/games/%s/rounds/%d/answers".formatted(id, round))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"answerState\":\"%s\",\"hintsUsed\":0,\"wrongGuesses\":0}".formatted(state)))
        .andExpect(status().isOk());
  }

  /** Every round is a different game type, so a wrong-guess limit of 0 is only valid for these. */
  private void finish(String id, AnswerState state, int rounds) throws Exception {
    for (int round = 0; round < rounds; round++) {
      answer(id, round, state);
    }
  }

  private JsonNode score() throws Exception {
    return score("");
  }

  private JsonNode score(String query) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(get("/api/daily-scores/" + DATE + query))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @Test
  void aDayWithNoFinishedGamesHasZeros() throws Exception {
    JsonNode score = score();

    assertThat(score.get("players").asInt()).isZero();
    assertThat(score.get("averageScore").isNull()).isTrue();
    assertThat(score.get("buckets")).hasSize(11);
  }

  @Test
  void finishedGamesAreCountedOnceAndAppearInTheEndpointAndTheView() throws Exception {
    String perfect = createDaily();
    finish(perfect, AnswerState.CORRECT, 5);
    String zero = createDaily();
    finish(zero, AnswerState.GAVE_UP, 5);
    String unfinished = createDaily();
    answer(unfinished, 0, AnswerState.CORRECT);

    // Repeating a final answer changes nothing.
    answer(perfect, 4, AnswerState.CORRECT);
    answer(zero, 4, AnswerState.GAVE_UP);

    JsonNode score = score();
    assertThat(score.get("players").asInt()).isEqualTo(2);
    assertThat(score.get("averageScore").asDouble()).isEqualTo(250.0);
    assertThat(score.get("maxScore").asInt()).isEqualTo(500);
    List<Integer> buckets = new ArrayList<>();
    score.get("buckets").forEach(n -> buckets.add(n.asInt()));
    assertThat(buckets).containsExactly(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1);

    Map<String, Object> row =
        jdbc.queryForMap(
            "select players, average_score, bucket_0, bucket_10 from daily_score_history"
                + " where challenge_date = ?",
            DATE);
    assertThat(((Number) row.get("players")).intValue()).isEqualTo(2);
    assertThat(((Number) row.get("average_score")).doubleValue()).isEqualTo(250.0);
    assertThat(((Number) row.get("bucket_0")).intValue()).isEqualTo(1);
    assertThat(((Number) row.get("bucket_10")).intValue()).isEqualTo(1);
  }

  @Test
  void theTotalsSurviveTheCleanupOfTheGames() throws Exception {
    finish(createDaily(), AnswerState.CORRECT, 5);
    jdbc.update("update game_entity set created_at = created_at - interval '3' day");

    cleanup.deleteOldGames();

    assertThat(score().get("players").asInt()).isEqualTo(1);
  }

  @Test
  void aRolledBackAnswerRollsBackTheCount() throws Exception {
    String id = createDaily();
    finish(id, AnswerState.CORRECT, 4);
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new IllegalStateException("boom after the count");
            })
        .when(store)
        .recordFinished(any(), any(), anyInt(), anyInt());

    assertThatThrownBy(() -> answer(id, 4, AnswerState.CORRECT))
        .hasRootCauseMessage("boom after the count");
    org.mockito.Mockito.reset(store);

    assertThat(score().get("players").asInt()).isZero();
    // The answer rolled back too, so the round can still be answered, and then it counts once.
    answer(id, 4, AnswerState.CORRECT);
    assertThat(score().get("players").asInt()).isEqualTo(1);
  }

  @Test
  void eachDifficultyIsItsOwnLeague() throws Exception {
    finish(createDaily(",\"difficulty\":\"EASY\""), AnswerState.CORRECT, 5);
    finish(createDaily(",\"difficulty\":\"HARD\""), AnswerState.GAVE_UP, 5);
    finish(createDaily(",\"difficulty\":\"HARD\""), AnswerState.GAVE_UP, 5);

    JsonNode easy = score("?difficulty=EASY");
    assertThat(easy.get("difficulty").asString()).isEqualTo("EASY");
    assertThat(easy.get("players").asInt()).isEqualTo(1);
    assertThat(easy.get("averageScore").asDouble()).isEqualTo(500.0);
    JsonNode hard = score("?difficulty=HARD");
    assertThat(hard.get("difficulty").asString()).isEqualTo("HARD");
    assertThat(hard.get("players").asInt()).isEqualTo(2);
    assertThat(hard.get("averageScore").asDouble()).isZero();
    assertThat(score("?difficulty=NORMAL").get("players").asInt()).isZero();
  }

  @Test
  void theDefaultLeagueIsNormalAndAnUnknownDifficultyCountsThere() throws Exception {
    // The request names no difficulty and no hardMode, so the game's difficulty is unknown.
    finish(createDaily(), AnswerState.CORRECT, 5);

    JsonNode score = score();

    assertThat(score.get("difficulty").asString()).isEqualTo("NORMAL");
    assertThat(score.get("players").asInt()).isEqualTo(1);
    assertThat(score("?difficulty=NORMAL").get("players").asInt()).isEqualTo(1);
    assertThat(score("?difficulty=EASY").get("players").asInt()).isZero();
  }

  @Test
  void anOldClientsHardModeCountsInTheHardLeague() throws Exception {
    finish(createDaily(",\"hardMode\":true"), AnswerState.CORRECT, 5);

    assertThat(score("?difficulty=HARD").get("players").asInt()).isEqualTo(1);
    assertThat(score().get("players").asInt()).isZero();
  }

  @Test
  void aBadDifficultyIsABadRequest() throws Exception {
    mockMvc
        .perform(get("/api/daily-scores/" + DATE + "?difficulty=EXTREME"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(get("/api/daily-scores/" + DATE + "?difficulty="))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aBadDateIsABadRequest() throws Exception {
    mockMvc.perform(get("/api/daily-scores/nonsense")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/daily-scores/2999-01-01")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/daily-scores/2001-01-01")).andExpect(status().isBadRequest());
  }

  @Test
  void aPartlyAnsweredRoundAddsItsPartialScoreToTheTotals() throws Exception {
    String id = createDaily();
    int expected = 0;
    for (int round = 0; round < 5; round++) {
      JsonNode type =
          objectMapper
              .readTree(
                  mockMvc
                      .perform(get("/api/games/%s/rounds/%d".formatted(id, round)))
                      .andExpect(status().isOk())
                      .andReturn()
                      .getResponse()
                      .getContentAsString())
              .get("gameType");
      int answerCount = type.get("answerCount").asInt();
      int found = answerCount - 1;
      expected +=
          com.parkinson.benjamin.geogames.model.Scoring.roundPoints(
              AnswerState.GAVE_UP, 0, type.get("hintCount").asInt(), found, answerCount);
      mockMvc
          .perform(
              post("/api/games/%s/rounds/%d/answers".formatted(id, round))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0,\"wrongGuesses\":0,\"answersFound\":%d}"
                          .formatted(found)))
          .andExpect(status().isOk());
    }

    JsonNode score = score();
    assertThat(score.get("players").asInt()).isEqualTo(1);
    assertThat(score.get("averageScore").asDouble()).isEqualTo((double) expected);
  }
}
