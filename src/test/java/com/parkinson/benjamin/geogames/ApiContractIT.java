package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.model.GameType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The shape of every JSON response the frontend reads, compared with the committed {@code
 * client/src/test/api-shapes.json}. The frontend's {@code client/src/api/contract.test.ts} checks its
 * fixtures against the same file, so a change to a response fails here until the file is
 * regenerated, and then fails there until the frontend is updated.
 *
 * <p>Regenerate with {@code mvn verify -Dcontract.update=true -Dit.test=ApiContractIT} and commit
 * the result. The shape format is described in {@link JsonShape}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractIT {

  private static final Path CONTRACT = Path.of("client", "src", "test", "api-shapes.json");
  private static final int ROUNDS = 10;

  /** Games merged per type, so rarely-present fields (such as aliases) show up. */
  private static final int GAMES = 5;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private String body(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc
        .perform(request)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private JsonNode createGame(GameType type) throws Exception {
    return objectMapper.readTree(
        body(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"gameType\":\"%s\",\"numberOfRounds\":%d}".formatted(type.name(), ROUNDS))));
  }

  /** The shape of the round responses of one game, with each {@code jsonBlob} parsed. */
  private Object roundShape(GameType type) throws Exception {
    Object merged = "empty";
    for (int game = 0; game < GAMES; game++) {
      merged = JsonShape.merge(merged, oneGameShape(type));
    }
    return merged;
  }

  private Object oneGameShape(GameType type) throws Exception {
    String id = createGame(type).get("id").asString();
    Object merged = "empty";
    for (int i = 0; i < ROUNDS; i++) {
      postGuess(id, i, firstName(type));
      postHint(id, i, 0, 1);
      JsonNode round = objectMapper.readTree(body(get("/api/games/%s/rounds/%d".formatted(id, i))));
      Map<String, Object> shape = new TreeMap<>();
      round
          .properties()
          .forEach(
              entry -> {
                if (!entry.getKey().equals("jsonBlob")) {
                  shape.put(entry.getKey(), JsonShape.of(entry.getValue()));
                }
              });
      shape.put("jsonBlob", JsonShape.of(objectMapper.readTree(round.get("jsonBlob").asString())));
      merged = JsonShape.merge(merged, shape);
    }
    return merged;
  }

  private static String namesPath(GameType type) {
    return switch (type.placeKind()) {
      case COUNTRIES -> "/api/countries";
      case RIVERS -> "/api/rivers";
      case LAKES -> "/api/lakes";
    };
  }

  /** A known name for the type, so the round carries a stored guess. */
  private String firstName(GameType type) throws Exception {
    return objectMapper.readTree(body(get(namesPath(type)))).get(0).get("name").asString();
  }

  private String postGuess(String id, int round, String guess) throws Exception {
    return body(
        post("/api/games/%s/rounds/%d/guesses".formatted(id, round))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"guess\":\"%s\",\"correct\":false}".formatted(guess)));
  }

  private String postHint(String id, int round, int hint, int guessesBefore) throws Exception {
    return body(
        post("/api/games/%s/rounds/%d/hints".formatted(id, round))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"hint\":%d,\"guessesBefore\":%d}".formatted(hint, guessesBefore)));
  }

  private Object hintShape() throws Exception {
    String id = createGame(GameType.TRIPOINT).get("id").asString();
    return JsonShape.of(objectMapper.readTree(postHint(id, 0, 0, 0)));
  }

  private Object guessShape() throws Exception {
    String id = createGame(GameType.TRIPOINT).get("id").asString();
    return JsonShape.of(objectMapper.readTree(postGuess(id, 0, firstName(GameType.TRIPOINT))));
  }

  private Object answerShape() throws Exception {
    String id = createGame(GameType.TRIPOINT).get("id").asString();
    return JsonShape.of(
        objectMapper.readTree(
            body(
                post("/api/games/%s/rounds/0/answers".formatted(id))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":0,\"wrongGuesses\":0}"))));
  }

  /** Finishes a daily game first, so {@code averageScore} is a number and not null. */
  private Object dailyScoreShape() throws Exception {
    JsonNode daily =
        objectMapper.readTree(
            body(post("/api/daily-games").contentType(MediaType.APPLICATION_JSON).content("{}")));
    for (int round = 0; round < 5; round++) {
      body(
          post("/api/games/%s/rounds/%d/answers".formatted(daily.get("id").asString(), round))
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0,\"wrongGuesses\":0}"));
    }
    return endpointShape("/api/daily-scores/" + daily.get("date").asString());
  }

  /** Creates the day first: the endpoint never generates a challenge. */
  private Object dailyChallengeShape() throws Exception {
    JsonNode daily =
        objectMapper.readTree(
            body(post("/api/daily-games").contentType(MediaType.APPLICATION_JSON).content("{}")));
    return endpointShape("/api/daily-challenges/" + daily.get("date").asString());
  }

  private void giveUpAllRounds(String id) throws Exception {
    for (int round = 0; round < ROUNDS; round++) {
      body(
          post("/api/games/%s/rounds/%d/answers".formatted(id, round))
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0,\"wrongGuesses\":0}"));
    }
  }

  /**
   * A challenge, its table and its public page. Five friends play it, so the last is below the top
   * five and its table has every field.
   */
  private void challengeShapes(Map<String, Object> endpoints) throws Exception {
    String creator = createGame(GameType.TRIPOINT).get("id").asString();
    giveUpAllRounds(creator);
    JsonNode challenge =
        objectMapper.readTree(body(post("/api/games/%s/challenges".formatted(creator))));
    String challengeId = challenge.get("id").asString();
    mockMvc
        .perform(
            put("/api/games/%s/challenge-initials".formatted(creator))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"initials\":\"abc\"}"))
        .andExpect(status().isNoContent());
    String friend = creator;
    for (int i = 0; i < 5; i++) {
      friend =
          objectMapper
              .readTree(body(post("/api/challenges/%s/games".formatted(challengeId))))
              .get("id")
              .asString();
      giveUpAllRounds(friend);
    }
    endpoints.put("POST /api/games/{id}/challenges", JsonShape.of(challenge));
    endpoints.put("GET /api/challenges/{id}", endpointShape("/api/challenges/" + challengeId));
    endpoints.put(
        "GET /api/games/{id}/challenge-table",
        endpointShape("/api/games/%s/challenge-table".formatted(friend)));
  }

  private Object endpointShape(String path) throws Exception {
    return JsonShape.of(objectMapper.readTree(body(get(path))));
  }

  private JsonNode currentShapes() throws Exception {
    Map<String, Object> endpoints = new TreeMap<>();
    endpoints.put("GET /api/countries", endpointShape("/api/countries"));
    endpoints.put("GET /api/rivers", endpointShape("/api/rivers"));
    endpoints.put("GET /api/lakes", endpointShape("/api/lakes"));
    endpoints.put("POST /api/games", JsonShape.of(createGame(GameType.TRIPOINT)));
    endpoints.put(
        "POST /api/daily-games",
        JsonShape.of(
            objectMapper.readTree(
                body(
                    post("/api/daily-games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")))));
    endpoints.put("GET /api/daily-challenges/{date}", dailyChallengeShape());
    challengeShapes(endpoints);
    endpoints.put("GET /api/daily-scores/{date}", dailyScoreShape());
    endpoints.put("POST /api/games/{id}/rounds/{round}/answers", answerShape());

    endpoints.put("POST /api/games/{id}/rounds/{round}/guesses", guessShape());
    endpoints.put("POST /api/games/{id}/rounds/{round}/hints", hintShape());

    Map<String, Object> rounds = new TreeMap<>();
    for (GameType type : GameType.values()) {
      rounds.put(type.name(), roundShape(type));
    }
    Map<String, Object> all = new TreeMap<>();
    all.put("endpoints", endpoints);
    all.put("rounds", rounds);
    return objectMapper.valueToTree(all);
  }

  @Test
  void responseShapesMatchTheCommittedContract() throws Exception {
    JsonNode actual = currentShapes();

    if (Boolean.getBoolean("contract.update")) {
      write(actual);
      return;
    }
    if (!Files.exists(CONTRACT)) {
      fail(
          "%s does not exist. Create it with: mvn verify -Dcontract.update=true -Dit.test=ApiContractIT"
              .formatted(CONTRACT));
    }
    JsonNode committed = objectMapper.readTree(Files.readString(CONTRACT));
    List<String> differences = new ArrayList<>();
    differences("", committed, actual, differences);
    if (!differences.isEmpty()) {
      fail(
          """
          The API responses no longer match %s:
            %s

          If the change is intended: run  mvn verify -Dcontract.update=true -Dit.test=ApiContractIT
          to regenerate the file, then update the frontend (client/src/api/contract.test.ts
          fails until its fixtures match) and commit both.\
          """
              .formatted(CONTRACT, String.join("\n  ", differences)));
    }
  }

  private void write(JsonNode shapes) throws IOException {
    Files.createDirectories(CONTRACT.getParent());
    DefaultPrettyPrinter printer =
        new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
    printer = printer.withArrayIndenter(new DefaultIndenter("  ", "\n"));
    Files.writeString(
        CONTRACT, objectMapper.writer().with(printer).writeValueAsString(shapes) + "\n");
  }

  /** Lists where two shape trees differ, as {@code path: committed -> current}. */
  private static void differences(
      String path, JsonNode committed, JsonNode actual, List<String> out) {
    if (committed.isObject() && actual.isObject()) {
      for (String key : committed.propertyNames()) {
        String child = path.isEmpty() ? key : path + "." + key;
        if (!actual.has(key)) {
          out.add(child + ": removed (was " + committed.get(key) + ")");
        } else {
          differences(child, committed.get(key), actual.get(key), out);
        }
      }
      for (String key : actual.propertyNames()) {
        if (!committed.has(key)) {
          out.add((path.isEmpty() ? key : path + "." + key) + ": added (" + actual.get(key) + ")");
        }
      }
    } else if (!committed.equals(actual) && !isUnknown(committed) && !isUnknown(actual)) {
      out.add(path + ": " + committed + " -> " + actual);
    }
  }

  /**
   * An array that was empty in every sampled response says nothing about its elements, so it
   * matches any shape. This keeps the check from depending on which random rounds were drawn.
   */
  private static boolean isUnknown(JsonNode shape) {
    return shape.isString() && shape.asString().equals("empty");
  }
}
