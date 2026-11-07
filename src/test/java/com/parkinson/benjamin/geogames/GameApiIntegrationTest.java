package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises the whole stack against the real processed data, with nothing mocked. */
@SpringBootTest
@AutoConfigureMockMvc
class GameApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private int createGame(String gameType, int rounds) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/games")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"gameType\":\"%s\",\"numberOfRounds\":%d}".formatted(gameType, rounds)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private String getRound(int gameId, int round) throws Exception {
    return mockMvc
        .perform(get("/api/games/%d/rounds/%d".formatted(gameId, round)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String answer(int gameId, int round, String state) throws Exception {
    return mockMvc
        .perform(
            post("/api/games/%d/rounds/%d/answers".formatted(gameId, round))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"%s\"}".formatted(state)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void playsAFullTripointGame() throws Exception {
    int id = createGame("TRIPOINT", 3);

    for (int i = 0; i < 3; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name")).isEqualTo("TRIPOINT");
      assertThat((int) JsonPath.read(round, "$.gameType.answerCount")).isEqualTo(3);
      assertThat((int) JsonPath.read(round, "$.totalRoundCount")).isEqualTo(3);
      assertThat((String) JsonPath.read(round, "$.gameResult.gameState")).isEqualTo("CREATED");

      String blob = JsonPath.read(round, "$.jsonBlob");
      assertThat((int) JsonPath.read(blob, "$.countries.length()")).isEqualTo(3);
      assertThat((List<?>) JsonPath.read(blob, "$.countries[*].name"))
          .hasSize(3)
          .doesNotContainNull();
      assertThat((List<?>) JsonPath.read(blob, "$.countries[*].additionalNames"))
          .hasSize(3)
          .doesNotContainNull();
      assertThat((List<?>) JsonPath.read(blob, "$.countries[*].geoData.geometry"))
          .hasSize(3)
          .doesNotContainNull();
    }

    String afterFirst = answer(id, 0, "CORRECT");
    assertThat((String) JsonPath.read(afterFirst, "$.gameResult.gameState"))
        .isEqualTo("IN_PROGRESS");
    assertThat((int) JsonPath.read(afterFirst, "$.gameResult.score")).isEqualTo(1);

    answer(id, 1, "GAVE_UP");
    String finished = answer(id, 2, "CORRECT");
    assertThat((String) JsonPath.read(finished, "$.gameResult.gameState")).isEqualTo("FINISHED");
    assertThat((int) JsonPath.read(finished, "$.gameResult.score")).isEqualTo(2);
    List<String> states = JsonPath.read(finished, "$.gameResult.roundAnswerStates");
    assertThat(states).containsExactly("CORRECT", "GAVE_UP", "CORRECT");
  }

  @Test
  void createsARiverGame() throws Exception {
    int id = createGame("RIVERS_BY_SHAPE", 2);

    for (int i = 0; i < 2; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name")).isEqualTo("RIVERS_BY_SHAPE");
      assertThat((int) JsonPath.read(round, "$.gameType.answerCount")).isEqualTo(1);

      String blob = JsonPath.read(round, "$.jsonBlob");
      assertThat((String) JsonPath.read(blob, "$.name")).isNotBlank();
      assertThat((String) JsonPath.read(blob, "$.continent")).isNotBlank();
      assertThat((Object) JsonPath.read(blob, "$.geoData")).isNotNull();
    }
  }

  @Test
  void createsARandomGame() throws Exception {
    int id = createGame("RANDOM", 10);

    for (int i = 0; i < 10; i++) {
      String round = getRound(id, i);
      assertThat((int) JsonPath.read(round, "$.totalRoundCount")).isEqualTo(10);
      assertThat((String) JsonPath.read(round, "$.gameType.name"))
          .isIn("TRIPOINT", "RIVERS_BY_SHAPE");
    }
  }

  @Test
  void listsEveryCountrySortedByName() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/countries"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<String> names = JsonPath.read(body, "$[*].name");
    assertThat(names).hasSize(TestFixtures.EXPECTED_COUNTRY_COUNT).isSorted();
  }

  private void postGame(String body, int expectedStatus) throws Exception {
    mockMvc
        .perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().is(expectedStatus));
  }

  @Test
  void rejectsBadGameRequestsWithABadRequest() throws Exception {
    postGame("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":0}", 400);
    postGame("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":11}", 400);
    postGame("{\"gameType\":\"NOPE\",\"numberOfRounds\":3}", 400);
  }

  @Test
  void rejectsAnUnansweredAnswerAndLeavesTheRoundAlone() throws Exception {
    int id = createGame("RIVERS_BY_SHAPE", 1);

    mockMvc
        .perform(
            post("/api/games/%d/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"UNANSWERED\"}"))
        .andExpect(status().isBadRequest());

    assertThat((String) JsonPath.read(getRound(id, 0), "$.answerState")).isEqualTo("UNANSWERED");
  }

  @Test
  void unknownGamesAndRoundsAreNotFound() throws Exception {
    int id = createGame("RIVERS_BY_SHAPE", 1);

    mockMvc.perform(get("/api/games/999999/rounds/0")).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/games/%d/rounds/99".formatted(id))).andExpect(status().isNotFound());
  }

  @Test
  void corsOriginsComeFromTheConfiguredProperty() throws Exception {
    mockMvc
        .perform(
            options("/api/games")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    mockMvc
        .perform(
            options("/api/countries")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }
}
