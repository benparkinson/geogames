package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.parkinson.benjamin.geogames.model.CountryCitiesRound;
import com.parkinson.benjamin.geogames.model.CountryPairRound;
import com.parkinson.benjamin.geogames.model.CountryRiversRound;
import com.parkinson.benjamin.geogames.model.CountryShapeRound;
import com.parkinson.benjamin.geogames.model.GameData;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.model.LakeRound;
import com.parkinson.benjamin.geogames.model.RiverRound;
import com.parkinson.benjamin.geogames.model.Tripoint;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/** Exercises the whole stack against the real processed data, with nothing mocked. */
@SpringBootTest
@AutoConfigureMockMvc
class GameApiIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Value("${geogames.cors.allowed-origins}")
  private String[] allowedOrigins;

  private String createGame(String gameType, int rounds) throws Exception {
    return TestFixtures.createGame(mockMvc, gameType, rounds);
  }

  private String getRound(String gameId, int round) throws Exception {
    return mockMvc
        .perform(get("/api/games/%s/rounds/%d".formatted(gameId, round)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String answer(String gameId, int round, String state) throws Exception {
    return answer(gameId, round, state, 0);
  }

  private String answer(String gameId, int round, String state, int hintsUsed) throws Exception {
    return answer(gameId, round, state, hintsUsed, validWrongGuesses(state));
  }

  /** A wrong-guess count that is valid for the state: every game type has a guess limit of 5. */
  private static int validWrongGuesses(String state) {
    return state.equals("OUT_OF_GUESSES") ? 5 : 0;
  }

  /** Posts an answer; a null {@code wrongGuesses} is left out of the JSON, like an old client. */
  private String answer(String gameId, int round, String state, int hintsUsed, Integer wrongGuesses)
      throws Exception {
    String wrong = wrongGuesses == null ? "" : ",\"wrongGuesses\":" + wrongGuesses;
    return mockMvc
        .perform(
            post("/api/games/%s/rounds/%d/answers".formatted(gameId, round))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"answerState\":\"%s\",\"hintsUsed\":%d%s}"
                        .formatted(state, hintsUsed, wrong)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void hardModeIsStoredWithTheGameAndReturnedOnEveryRound() throws Exception {
    String id = TestFixtures.createGame(mockMvc, "TRIPOINT", 3, ",\"hardMode\":true");

    for (int round = 0; round < 3; round++) {
      assertThat((Boolean) JsonPath.read(getRound(id, round), "$.hardMode")).isTrue();
    }
  }

  @Test
  void aGameCreatedWithoutHardModeReturnsFalse() throws Exception {
    String id = createGame("TRIPOINT", 3);

    for (int round = 0; round < 3; round++) {
      assertThat((Boolean) JsonPath.read(getRound(id, round), "$.hardMode")).isFalse();
    }
  }

  @Test
  void playsAFullTripointGame() throws Exception {
    String id = createGame("TRIPOINT", 3);
    assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

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
    assertThat((List<Boolean>) JsonPath.read(finished, "$.gameResult.roundFirstGuess"))
        .containsExactly(true, false, false);
    assertThat((int) JsonPath.read(finished, "$.gameResult.firstGuessRounds")).isEqualTo(1);
  }

  @Test
  void aWinAfterAWrongGuessIsNotAFirstGuess() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 2);

    answer(id, 0, "CORRECT", 0, 0);
    String result = answer(id, 1, "CORRECT", 0, 2);

    assertThat((List<Boolean>) JsonPath.read(result, "$.gameResult.roundFirstGuess"))
        .containsExactly(true, false);
    assertThat((int) JsonPath.read(result, "$.gameResult.firstGuessRounds")).isEqualTo(1);
    // both rounds score the same: the outcome is only a label
    assertThat((int) JsonPath.read(result, "$.gameResult.score")).isEqualTo(200);
  }

  @Test
  void acceptsOutOfGuesses() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);

    String result = answer(id, 0, "OUT_OF_GUESSES");

    assertThat((String) JsonPath.read(result, "$.answerState")).isEqualTo("OUT_OF_GUESSES");
    assertThat((int) JsonPath.read(result, "$.points")).isZero();
  }

  @Test
  void theAnswerResponseHasNoRoundData() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);

    String result = answer(id, 0, "CORRECT");

    assertThat(objectMapper.readTree(result).propertyNames())
        .containsExactlyInAnyOrder("answerState", "hintsUsed", "points", "gameResult");
  }

  @Test
  void createsARiverGame() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 2);

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
  void createsARiverByCountriesGameWithTheCountriesOfEachRiver() throws Exception {
    String id = createGame("RIVERS_BY_COUNTRIES", 10);

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
  void createsALakeShapesGame() throws Exception {
    String id = createGame("LAKES_BY_SHAPE", 10);

    for (int i = 0; i < 10; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name")).isEqualTo("LAKES_BY_SHAPE");
      assertThat((int) JsonPath.read(round, "$.gameType.answerCount")).isEqualTo(1);
      assertThat((int) JsonPath.read(round, "$.gameType.hintCount")).isEqualTo(3);
      assertThat((int) JsonPath.read(round, "$.gameType.guessLimit")).isEqualTo(5);

      String blob = JsonPath.read(round, "$.jsonBlob");
      assertThat((String) JsonPath.read(blob, "$.lake.name")).isNotBlank();
      assertThat((String) JsonPath.read(blob, "$.lake.geoData.geometry.type"))
          .isIn("Polygon", "MultiPolygon");
      assertThat((List<?>) JsonPath.read(blob, "$.lake.countries")).isNotEmpty();
    }
  }

  @Test
  void createsACountriesByNeighboursGame() throws Exception {
    String id = createGame("COUNTRIES_BY_NEIGHBOURS", 10);

    for (int i = 0; i < 10; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name"))
          .isEqualTo("COUNTRIES_BY_NEIGHBOURS");
      assertThat((int) JsonPath.read(round, "$.gameType.hintCount")).isEqualTo(3);

      String blob = JsonPath.read(round, "$.jsonBlob");
      assertThat((String) JsonPath.read(blob, "$.country.name")).isNotBlank();
      assertThat((Object) JsonPath.read(blob, "$.country.outline")).isNotNull();
      assertThat((List<?>) JsonPath.read(blob, "$.neighbours")).isNotEmpty();
    }
  }

  @Test
  void createsACountriesByCoastlineGameWithoutNeighbours() throws Exception {
    String id = createGame("COUNTRIES_BY_COASTLINE", 10);

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
  void createsACountriesByLandBordersGameWithNeighbours() throws Exception {
    String id = createGame("COUNTRIES_BY_LAND_BORDERS", 10);

    for (int i = 0; i < 10; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name"))
          .isEqualTo("COUNTRIES_BY_LAND_BORDERS");

      String blob = JsonPath.read(round, "$.jsonBlob");
      assertThat((List<?>) JsonPath.read(blob, "$.country.landBorders.geometry.coordinates"))
          .isNotEmpty();
      assertThat((List<?>) JsonPath.read(blob, "$.neighbours")).isNotEmpty();
    }
  }

  @Test
  void createsARandomGame() throws Exception {
    String id = createGame("RANDOM", 10);
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
      } else if (type.startsWith("LAKES_")) {
        subjects.add("lake:" + JsonPath.read(blob, "$.lake.id"));
      } else if (type.equals("BORDERS_BY_SHAPE") || type.equals("MERGED_COUNTRIES")) {
        List<String> names = JsonPath.read(blob, "$.countries[*].name");
        subjects.add("pair:" + names.stream().sorted().collect(Collectors.joining("|")));
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

    String id = createGame("RIVERS_BY_SHAPE", 5);
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
    String id = createGame("RIVERS_BY_SHAPE", 1);

    mockMvc
        .perform(
            post("/api/games/%s/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"UNANSWERED\"}"))
        .andExpect(status().isBadRequest());

    assertThat((String) JsonPath.read(getRound(id, 0), "$.answerState")).isEqualTo("UNANSWERED");
  }

  @Test
  void rejectsMoreHintsThanTheTypeHas() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);

    mockMvc
        .perform(
            post("/api/games/%s/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":9}"))
        .andExpect(status().isBadRequest());

    assertThat((String) JsonPath.read(getRound(id, 0), "$.answerState")).isEqualTo("UNANSWERED");
  }

  @Test
  void rejectsOutOfGuessesWithFewerWrongGuessesThanTheLimit() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);

    mockMvc
        .perform(
            post("/api/games/%s/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"OUT_OF_GUESSES\",\"wrongGuesses\":4}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").isString());

    assertThat((String) JsonPath.read(getRound(id, 0), "$.answerState")).isEqualTo("UNANSWERED");
  }

  @Test
  void anAnswerWithoutWrongGuessesIsStillAccepted() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);

    String body = answer(id, 0, "CORRECT", 0, null);

    assertThat((int) JsonPath.read(body, "$.points")).isEqualTo(100);
  }

  @Test
  void aRepeatThatDiffersOnlyInWrongGuessesIsAConflict() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);

    answer(id, 0, "CORRECT", 0, 2);
    answer(id, 0, "CORRECT", 0, 2);

    mockMvc
        .perform(
            post("/api/games/%s/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":0,\"wrongGuesses\":3}"))
        .andExpect(status().isConflict());
  }

  @Test
  void anAnswerWithoutHintsUsedCountsAsNoHints() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);

    String body =
        mockMvc
            .perform(
                post("/api/games/%s/rounds/0/answers".formatted(id))
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
    String id = createGame("RIVERS_BY_SHAPE", 1);

    mockMvc.perform(get("/api/games/999999/rounds/0")).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/games/%s/rounds/99".formatted(id))).andExpect(status().isNotFound());
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

  /** The record a round of this type is stored as. No default, so a new type fails to compile. */
  private static Class<? extends GameData> dataClass(GameType type) {
    return switch (type) {
      case TRIPOINT -> Tripoint.class;
      case RIVERS_BY_SHAPE, RIVERS_BY_COUNTRIES -> RiverRound.class;
      case COUNTRIES_BY_NEIGHBOURS, COUNTRIES_BY_COASTLINE, COUNTRIES_BY_LAND_BORDERS ->
          CountryShapeRound.class;
      case LAKES_BY_SHAPE -> LakeRound.class;
      case BORDERS_BY_SHAPE, MERGED_COUNTRIES -> CountryPairRound.class;
      case COUNTRIES_BY_CITIES -> CountryCitiesRound.class;
      case COUNTRIES_BY_RIVERS -> CountryRiversRound.class;
    };
  }

  @ParameterizedTest
  @EnumSource(GameType.class)
  void everyGameTypeProducesRoundsThatDeserialiseIntoItsGameData(GameType type) throws Exception {
    String id = createGame(type.name(), 10);
    Set<String> subjects = new HashSet<>();

    for (int i = 0; i < 10; i++) {
      String round = getRound(id, i);
      assertThat((String) JsonPath.read(round, "$.gameType.name")).isEqualTo(type.name());

      GameData data =
          objectMapper.readValue((String) JsonPath.read(round, "$.jsonBlob"), dataClass(type));
      assertThat(data.gameType()).isEqualTo(type);
      subjects.add(data.subjectId());
    }
    assertThat(subjects).hasSize(10);
  }

  @Test
  void answeringTheSameWayTwiceChangesNothingButAConflictingAnswerIsRejected() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 2);

    String first = answer(id, 0, "CORRECT", 1);
    String again = answer(id, 0, "CORRECT", 1);

    assertThat((int) JsonPath.read(again, "$.gameResult.score"))
        .isEqualTo(JsonPath.<Integer>read(first, "$.gameResult.score"));
    assertThat((int) JsonPath.read(again, "$.points")).isEqualTo(83);

    mockMvc
        .perform(
            post("/api/games/%s/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").isString())
        .andExpect(jsonPath("$.detail").isString());
    assertThat((String) JsonPath.read(getRound(id, 0), "$.answerState")).isEqualTo("CORRECT");
  }

  @Test
  void badRequestsGetProblemDetailBodies() throws Exception {
    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isString());
  }

  @Test
  void malformedJsonIsABadRequest() throws Exception {
    mockMvc
        .perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        // Current behaviour: a 400 with an empty body, not a ProblemDetail like the other 400s,
        // because ApiExceptionHandler does not handle HttpMessageNotReadableException.
        .andExpect(status().isBadRequest())
        .andExpect(content().string(""));
  }

  @Test
  void aPlainTextBodyIsUnsupportedMediaType() throws Exception {
    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.TEXT_PLAIN)
                .content("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":1}"))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void answeringAnUnknownGameIsNotFound() throws Exception {
    mockMvc
        .perform(
            post("/api/games/999999/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"GAVE_UP\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void listsEveryLakeAndCoversTheLakesInAGame() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/lakes"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<String> names = JsonPath.read(body, "$[*].name");
    assertThat(names).hasSize(lakeFileCount()).isSorted();

    String id = createGame("LAKES_BY_SHAPE", 5);
    for (int i = 0; i < 5; i++) {
      String blob = JsonPath.read(getRound(id, i), "$.jsonBlob");
      assertThat(names).contains(JsonPath.<String>read(blob, "$.lake.name"));
    }
  }

  private static int lakeFileCount() throws IOException {
    return new PathMatchingResourcePatternResolver()
        .getResources("classpath:processed/lakes/*.json")
        .length;
  }

  @Test
  void everyConfiguredOriginMayPreflightTheGameAndPlaceNameEndpoints() throws Exception {
    assertThat(allowedOrigins).isNotEmpty();
    for (String origin : allowedOrigins) {
      mockMvc
          .perform(
              options("/api/games")
                  .header("Origin", origin)
                  .header("Access-Control-Request-Method", "POST"))
          .andExpect(status().isOk())
          .andExpect(header().string("Access-Control-Allow-Origin", origin));
      mockMvc
          .perform(
              options("/api/countries")
                  .header("Origin", origin)
                  .header("Access-Control-Request-Method", "GET"))
          .andExpect(status().isOk())
          .andExpect(header().string("Access-Control-Allow-Origin", origin));
    }
  }

  @Test
  void theProductionFrontendOriginIsAllowed() throws Exception {
    // https://geogauntlet.onrender.com is the production frontend. Removing it from
    // geogames.cors.allowed-origins breaks production while every other test still passes.
    String production = "https://geogauntlet.onrender.com";
    mockMvc
        .perform(
            options("/api/games")
                .header("Origin", production)
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", production));
  }

  private int postGuess(String id, int round, String guess, boolean correct) throws Exception {
    return mockMvc
        .perform(
            post("/api/games/%s/rounds/%d/guesses".formatted(id, round))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"guess\":\"%s\",\"correct\":%b}".formatted(guess, correct)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void aGuessIsKeptUntilTheRoundIsAnswered() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);
    assertThat((List<?>) JsonPath.read(getRound(id, 0), "$.guesses")).isEmpty();

    assertThat(postGuess(id, 0, "nile", false)).isEqualTo(200);
    assertThat(postGuess(id, 0, "NILE", false)).isEqualTo(200);
    assertThat(postGuess(id, 0, "Atlantis", false)).isEqualTo(400);

    String round = getRound(id, 0);
    assertThat((List<?>) JsonPath.read(round, "$.guesses")).hasSize(1);
    assertThat((String) JsonPath.read(round, "$.guesses[0].text")).isEqualTo("Nile");
    assertThat((Boolean) JsonPath.read(round, "$.guesses[0].correct")).isFalse();

    answer(id, 0, "GAVE_UP");

    assertThat((List<?>) JsonPath.read(getRound(id, 0), "$.guesses")).hasSize(1);
    assertThat(postGuess(id, 0, "Congo", false)).isEqualTo(409);
  }

  @Test
  void aGuessOnAnUnknownGameIsNotFound() throws Exception {
    assertThat(postGuess("0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d", 0, "Nile", false)).isEqualTo(404);
  }

  @Test
  void guessesMadeAtTheSameMomentAreNotLost() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);
    List<String> names = List.of("Nile", "Amazon", "Danube", "Rhine");
    java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(names.size())) {
      List<java.util.concurrent.Future<Integer>> results =
          names.stream()
              .map(
                  name ->
                      pool.submit(
                          () -> {
                            start.await();
                            return postGuess(id, 0, name, false);
                          }))
              .toList();
      start.countDown();
      int stored = 0;
      for (var result : results) {
        int status = result.get();
        // A second lost race is a 409, which the client may simply retry.
        assertThat(status).isIn(200, 409);
        if (status == 200) {
          stored++;
        }
      }
      assertThat((List<?>) JsonPath.read(getRound(id, 0), "$.guesses")).hasSize(stored);
      assertThat(stored).isPositive();
    }
  }

  private int postHint(String id, int round, int hint, int guessesBefore) throws Exception {
    return mockMvc
        .perform(
            post("/api/games/%s/rounds/%d/hints".formatted(id, round))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hint\":%d,\"guessesBefore\":%d}".formatted(hint, guessesBefore)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void aHintIsKeptAndStillCostsPointsWhenTheAnswerReportsNone() throws Exception {
    String id = createGame("RIVERS_BY_SHAPE", 1);
    assertThat((List<?>) JsonPath.read(getRound(id, 0), "$.hintsTakenAt")).isEmpty();

    assertThat(postHint(id, 0, 0, 0)).isEqualTo(200);
    assertThat(postHint(id, 0, 0, 0)).isEqualTo(200);
    assertThat(postHint(id, 0, 2, 0)).isEqualTo(400);

    String round = getRound(id, 0);
    assertThat((List<Integer>) JsonPath.read(round, "$.hintsTakenAt")).containsExactly(0);
    assertThat((int) JsonPath.read(round, "$.hintsUsed")).isZero();

    String result = answer(id, 0, "CORRECT", 0);

    int hintCount = GameType.RIVERS_BY_SHAPE.hintCount();
    assertThat((int) JsonPath.read(result, "$.hintsUsed")).isEqualTo(1);
    assertThat((int) JsonPath.read(result, "$.points"))
        .isEqualTo(
            com.parkinson.benjamin.geogames.model.Scoring.roundPoints(
                com.parkinson.benjamin.geogames.model.AnswerState.CORRECT, 1, hintCount, 1, 1));
    assertThat(postHint(id, 0, 1, 0)).isEqualTo(409);
  }

  private int postAnswerWithFound(String id, String state, int found) throws Exception {
    return mockMvc
        .perform(
            post("/api/games/%s/rounds/0/answers".formatted(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"answerState\":\"%s\",\"hintsUsed\":0,\"wrongGuesses\":0,\"answersFound\":%d}"
                        .formatted(state, found)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void aTripointGivenUpAfterTwoCorrectGuessesEarnsTwoThirds() throws Exception {
    String id = createGame("TRIPOINT", 1);

    assertThat(postAnswerWithFound(id, "GAVE_UP", 3)).isEqualTo(400);
    assertThat(postAnswerWithFound(id, "CORRECT", 2)).isEqualTo(400);
    assertThat(postAnswerWithFound(id, "GAVE_UP", 2)).isEqualTo(200);

    String round = getRound(id, 0);
    assertThat((String) JsonPath.read(round, "$.answerState")).isEqualTo("GAVE_UP");
    assertThat((int) JsonPath.read(round, "$.points")).isEqualTo(67);
    assertThat((int) JsonPath.read(round, "$.gameResult.score")).isEqualTo(67);
    assertThat((int) JsonPath.read(round, "$.gameResult.roundsCorrect")).isZero();
    // A retry is harmless; a different count is a conflict.
    assertThat(postAnswerWithFound(id, "GAVE_UP", 2)).isEqualTo(200);
    assertThat(postAnswerWithFound(id, "GAVE_UP", 1)).isEqualTo(409);
  }
}
