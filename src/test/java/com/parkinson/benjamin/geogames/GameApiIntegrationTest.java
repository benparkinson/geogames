package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.parkinson.benjamin.geogames.model.GameType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
    return answer(gameId, round, state, 0);
  }

  private String answer(int gameId, int round, String state, int hintsUsed) throws Exception {
    return mockMvc
        .perform(
            post("/api/games/%d/rounds/%d/answers".formatted(gameId, round))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"%s\",\"hintsUsed\":%d}".formatted(state, hintsUsed)))
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
      assertThat((int) JsonPath.read(round, "$.gameType.hintCount")).isEqualTo(3);
      assertThat((int) JsonPath.read(round, "$.gameType.guessLimit")).isEqualTo(5);
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
    assertThat((int) JsonPath.read(afterFirst, "$.gameResult.score")).isEqualTo(100);
    assertThat((int) JsonPath.read(afterFirst, "$.points")).isEqualTo(100);

    answer(id, 1, "GAVE_UP");
    String finished = answer(id, 2, "CORRECT", 1);
    assertThat((String) JsonPath.read(finished, "$.gameResult.gameState")).isEqualTo("FINISHED");
    // a tripoint round answered with 1 of 3 hints is worth 83
    assertThat((int) JsonPath.read(finished, "$.hintsUsed")).isEqualTo(1);
    assertThat((int) JsonPath.read(finished, "$.points")).isEqualTo(83);
    assertThat((int) JsonPath.read(finished, "$.gameResult.score")).isEqualTo(183);
    assertThat((int) JsonPath.read(finished, "$.gameResult.maxScore")).isEqualTo(300);
    assertThat((int) JsonPath.read(finished, "$.gameResult.roundsCorrect")).isEqualTo(2);
    assertThat((int) JsonPath.read(finished, "$.gameResult.perfectRounds")).isEqualTo(1);
    assertThat((List<Integer>) JsonPath.read(finished, "$.gameResult.roundPoints"))
        .containsExactly(100, 0, 83);
    assertThat((List<Integer>) JsonPath.read(finished, "$.gameResult.roundHintsUsed"))
        .containsExactly(0, 0, 1);
    List<String> states = JsonPath.read(finished, "$.gameResult.roundAnswerStates");
    assertThat(states).containsExactly("CORRECT", "GAVE_UP", "CORRECT");
  }

  @Test
  void acceptsOutOfGuesses() throws Exception {
    int id = createGame("RIVERS_BY_SHAPE", 1);

    String result = answer(id, 0, "OUT_OF_GUESSES");

    assertThat((String) JsonPath.read(result, "$.answerState")).isEqualTo("OUT_OF_GUESSES");
    assertThat((int) JsonPath.read(result, "$.points")).isZero();
  }

  @Test
  void createsARiverGame() throws Exception {
    int id = createGame("RIVERS_BY_SHAPE", 2);

    for (int i = 0; i < 2; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name")).isEqualTo("RIVERS_BY_SHAPE");
      assertThat((int) JsonPath.read(round, "$.gameType.answerCount")).isEqualTo(1);

      String blob = JsonPath.read(round, "$.jsonBlob");
      assertThat((String) JsonPath.read(blob, "$.river.name")).isNotBlank();
      assertThat((String) JsonPath.read(blob, "$.river.continent")).isNotBlank();
      assertThat((Object) JsonPath.read(blob, "$.river.geoData")).isNotNull();
    }
  }

  @Test
  void createsARiverOnMapGame() throws Exception {
    int id = createGame("RIVERS_ON_MAP", 2);

    for (int i = 0; i < 2; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name")).isEqualTo("RIVERS_ON_MAP");
      assertThat((int) JsonPath.read(round, "$.gameType.answerCount")).isEqualTo(1);

      String blob = JsonPath.read(round, "$.jsonBlob");
      assertThat((String) JsonPath.read(blob, "$.river.name")).isNotBlank();
      assertThat((Object) JsonPath.read(blob, "$.river.geoData")).isNotNull();
      assertThat((Object) JsonPath.read(blob, "$.river.source")).isNotNull();
      assertThat((Object) JsonPath.read(blob, "$.river.mouth")).isNotNull();
      assertThat((List<?>) JsonPath.read(blob, "$.river.countries")).isNotNull();
    }
  }

  @Test
  void createsARiverByEndsGame() throws Exception {
    int id = createGame("RIVERS_BY_ENDS", 3);

    for (int i = 0; i < 3; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name")).isEqualTo("RIVERS_BY_ENDS");
      assertThat((int) JsonPath.read(round, "$.gameType.answerCount")).isEqualTo(1);

      String blob = JsonPath.read(round, "$.jsonBlob");
      assertThat((Double) JsonPath.read(blob, "$.river.source.latitude")).isBetween(-90.0, 90.0);
      assertThat((Double) JsonPath.read(blob, "$.river.mouth.longitude")).isBetween(-180.0, 180.0);
    }
  }

  @Test
  void createsARiverByCountriesGameWithTheCountriesOfEachRiver() throws Exception {
    int id = createGame("RIVERS_BY_COUNTRIES", 10);

    for (int i = 0; i < 10; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name")).isEqualTo("RIVERS_BY_COUNTRIES");

      String blob = JsonPath.read(round, "$.jsonBlob");
      List<String> riverCountries = JsonPath.read(blob, "$.river.countries");
      List<String> sent = JsonPath.read(blob, "$.countries[*].name");
      assertThat(riverCountries).hasSizeGreaterThanOrEqualTo(2);
      assertThat(sent).isEqualTo(riverCountries);
      assertThat((List<?>) JsonPath.read(blob, "$.countries[*].geoData.geometry"))
          .hasSize(sent.size())
          .doesNotContainNull();
    }
  }

  @Test
  void createsACountriesByCoastlineGameWithoutNeighbours() throws Exception {
    int id = createGame("COUNTRIES_BY_COASTLINE", 10);

    for (int i = 0; i < 10; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name"))
          .isEqualTo("COUNTRIES_BY_COASTLINE");

      String blob = JsonPath.read(round, "$.jsonBlob");
      assertThat((String) JsonPath.read(blob, "$.country.name")).isNotBlank();
      assertThat((List<?>) JsonPath.read(blob, "$.country.coast.geometry.coordinates"))
          .isNotEmpty();
      assertThat((List<?>) JsonPath.read(blob, "$.neighbours")).isEmpty();
    }
  }

  @Test
  void createsARandomGame() throws Exception {
    int id = createGame("RANDOM", 10);
    Set<String> subjects = new HashSet<>();

    for (int i = 0; i < 10; i++) {
      String round = getRound(id, i);
      assertThat((int) JsonPath.read(round, "$.totalRoundCount")).isEqualTo(10);
      assertThat((String) JsonPath.read(round, "$.gameType.name"))
          .isIn(Arrays.stream(GameType.values()).map(GameType::name).toList());

      String blob = JsonPath.read(round, "$.jsonBlob");
      String type = JsonPath.read(round, "$.gameType.name");
      if (type.equals("TRIPOINT")) {
        List<String> names = JsonPath.read(blob, "$.countries[*].name");
        subjects.add("tripoint:" + names.stream().sorted().collect(Collectors.joining("|")));
      } else if (type.startsWith("COUNTRIES_")) {
        subjects.add("country:" + JsonPath.read(blob, "$.country.name"));
      } else {
        subjects.add("river:" + JsonPath.read(blob, "$.river.id"));
      }
    }
    assertThat(subjects).hasSize(10);
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

  @Test
  void listsEveryRiverAndCoversTheRiversInAGame() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/rivers"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<String> names = JsonPath.read(body, "$[*].name");
    assertThat(names).hasSize(50).isSorted();

    int id = createGame("RIVERS_BY_SHAPE", 5);
    for (int i = 0; i < 5; i++) {
      String blob = JsonPath.read(getRound(id, i), "$.jsonBlob");
      String riverName = JsonPath.read(blob, "$.river.name");
      assertThat(names).contains(riverName);
    }
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
  void rejectsMoreHintsThanTheTypeHas() throws Exception {
    int id = createGame("RIVERS_BY_SHAPE", 1);

    mockMvc
        .perform(
            post("/api/games/%d/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":9}"))
        .andExpect(status().isBadRequest());

    assertThat((String) JsonPath.read(getRound(id, 0), "$.answerState")).isEqualTo("UNANSWERED");
  }

  @Test
  void anAnswerWithoutHintsUsedCountsAsNoHints() throws Exception {
    int id = createGame("RIVERS_BY_SHAPE", 1);

    String body =
        mockMvc
            .perform(
                post("/api/games/%d/rounds/0/answers".formatted(id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"answerState\":\"CORRECT\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat((int) JsonPath.read(body, "$.points")).isEqualTo(100);
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
