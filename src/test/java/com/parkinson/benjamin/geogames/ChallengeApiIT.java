package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.dao.GameRepository;
import com.parkinson.benjamin.geogames.model.GameOrigin;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "geogames.rate-limit.games-per-window=1000")
class ChallengeApiIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcClient jdbc;
  @Autowired private GameRepository games;

  private String finishedGame(int rounds) throws Exception {
    String id = TestFixtures.createGame(mockMvc, "TRIPOINT", rounds);
    for (int round = 0; round < rounds; round++) {
      mockMvc
          .perform(
              post("/api/games/%s/rounds/%d/answers".formatted(id, round))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0,\"wrongGuesses\":0}"))
          .andExpect(status().isOk());
    }
    return id;
  }

  private String body(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder r)
      throws Exception {
    return mockMvc
        .perform(r)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private JsonNode blob(String gameId, int round) throws Exception {
    JsonNode response =
        objectMapper.readTree(body(get("/api/games/%s/rounds/%d".formatted(gameId, round))));
    return objectMapper.readTree(response.get("jsonBlob").asString());
  }

  private String createChallenge(String gameId) throws Exception {
    return objectMapper
        .readTree(body(post("/api/games/%s/challenges".formatted(gameId))))
        .get("id")
        .asString();
  }

  @Test
  void aFriendGetsTheSameRoundsInANewGameWithNoAnswers() throws Exception {
    String source = finishedGame(2);
    String challenge = createChallenge(source);

    mockMvc
        .perform(get("/api/challenges/" + challenge))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameMode").value("TRIPOINT"))
        .andExpect(jsonPath("$.difficulty").value("NORMAL"))
        .andExpect(jsonPath("$.roundCount").value(2))
        .andExpect(jsonPath("$.score").value(0))
        .andExpect(jsonPath("$.maxScore").value(20))
        .andExpect(jsonPath("$.rounds").doesNotExist());

    String first =
        objectMapper
            .readTree(body(post("/api/challenges/%s/games".formatted(challenge))))
            .get("id")
            .asString();
    String second =
        objectMapper
            .readTree(body(post("/api/challenges/%s/games".formatted(challenge))))
            .get("id")
            .asString();

    assertThat(first).isNotEqualTo(second).isNotEqualTo(source);
    var copied = games.findByPublicId(UUID.fromString(first)).orElseThrow();
    assertThat(copied.getChallengeScore()).isZero();
    assertThat(copied.origin()).isEqualTo(GameOrigin.CHALLENGE);
    for (String copy : new String[] {first, second}) {
      for (int round = 0; round < 2; round++) {
        assertThat(blob(copy, round)).isEqualTo(blob(source, round));
        JsonNode state =
            objectMapper.readTree(body(get("/api/games/%s/rounds/%d".formatted(copy, round))));
        assertThat(state.get("answerState").asString()).isEqualTo("UNANSWERED");
        assertThat(state.get("hintsUsed").asInt()).isZero();
      }
    }
  }

  @Test
  void anUnfinishedGameIsAConflictAndAnUnknownOneIsNotFound() throws Exception {
    String unfinished = TestFixtures.createGame(mockMvc, "TRIPOINT", 2);

    mockMvc
        .perform(post("/api/games/%s/challenges".formatted(unfinished)))
        .andExpect(status().isConflict());
    mockMvc
        .perform(post("/api/games/%s/challenges".formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound());
  }

  @Test
  void aDailyGameCannotBeChallenged() throws Exception {
    String daily =
        objectMapper
            .readTree(
                body(
                    post("/api/daily-games").contentType(MediaType.APPLICATION_JSON).content("{}")))
            .get("id")
            .asString();
    for (int round = 0; round < 5; round++) {
      mockMvc
          .perform(
              post("/api/games/%s/rounds/%d/answers".formatted(daily, round))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"answerState\":\"GAVE_UP\",\"hintsUsed\":0,\"wrongGuesses\":0}"))
          .andExpect(status().isOk());
    }

    mockMvc
        .perform(post("/api/games/%s/challenges".formatted(daily)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aMalformedIdIsBadRequestAndAnUnknownChallengeIsNotFound() throws Exception {
    mockMvc.perform(get("/api/challenges/not-a-uuid")).andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/challenges/not-a-uuid/games")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/challenges/" + UUID.randomUUID())).andExpect(status().isNotFound());
    mockMvc
        .perform(post("/api/challenges/%s/games".formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound());
  }

  private int rows(String table) {
    return jdbc.sql("select count(*) from " + table).query(Integer.class).single();
  }

  private void answer(String game, int round, String state) throws Exception {
    mockMvc
        .perform(
            post("/api/games/%s/rounds/%d/answers".formatted(game, round))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"answerState\":\"%s\",\"hintsUsed\":0,\"wrongGuesses\":0}".formatted(state)))
        .andExpect(status().isOk());
  }

  private ResultActions initials(String game, String initials) throws Exception {
    return mockMvc.perform(
        put("/api/games/%s/challenge-initials".formatted(game))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"initials\":\"%s\"}".formatted(initials)));
  }

  @Test
  void theTopFiveCanEnterInitialsWhichTheTableAndTheChallengeShow() throws Exception {
    String creator = finishedGame(1);
    String challenge = createChallenge(creator);
    List<String> players = new ArrayList<>(List.of(creator));
    for (int i = 0; i < 5; i++) {
      String friend =
          objectMapper
              .readTree(body(post("/api/challenges/%s/games".formatted(challenge))))
              .get("id")
              .asString();
      // Everyone scores 0, so the order of finishing is the order of the table.
      answer(friend, 0, "GAVE_UP");
      players.add(friend);
    }

    initials(players.get(4), "abc").andExpect(status().isNoContent());
    initials(players.get(5), "xyz").andExpect(status().isConflict());
    initials(players.get(4), "xyz").andExpect(status().isConflict());
    initials(players.get(4), "a1").andExpect(status().isBadRequest());

    mockMvc
        .perform(get("/api/games/%s/challenge-table".formatted(players.get(4))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.challengeId").value(challenge))
        .andExpect(jsonPath("$.entryCount").value(6))
        .andExpect(jsonPath("$.creator").value(false))
        .andExpect(jsonPath("$.entries.length()").value(5))
        .andExpect(jsonPath("$.entries[4].initials").value("ABC"))
        .andExpect(jsonPath("$.entries[4].you").value(true))
        .andExpect(jsonPath("$.yours").value(nullValue()))
        .andExpect(jsonPath("$.canEnterInitials").value(false));
    mockMvc
        .perform(get("/api/games/%s/challenge-table".formatted(players.get(5))))
        .andExpect(jsonPath("$.yours.rank").value(6))
        .andExpect(jsonPath("$.canEnterInitials").value(false));
    mockMvc
        .perform(get("/api/games/%s/challenge-table".formatted(creator)))
        .andExpect(jsonPath("$.creator").value(true));
    mockMvc
        .perform(get("/api/challenges/" + challenge))
        .andExpect(jsonPath("$.entryCount").value(6))
        .andExpect(jsonPath("$.entries.length()").value(5))
        .andExpect(jsonPath("$.entries[4].initials").value("ABC"))
        .andExpect(jsonPath("$.entries[4].you").doesNotExist());
  }

  @Test
  void aGameWithNoChallengeHasNoTable() throws Exception {
    mockMvc
        .perform(get("/api/games/%s/challenge-table".formatted(finishedGame(1))))
        .andExpect(status().isNotFound());
  }

  @Test
  void theSiteMayPutInitialsFromItsOwnOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/games/%s/challenge-initials".formatted(UUID.randomUUID()))
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "PUT"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
  }

  @Test
  void aPlayedChallengeGetsAnEntryAndIsSharedOnUnderTheSameLink() throws Exception {
    String source = finishedGame(2);
    int challengesBefore = rows("challenge");
    int entriesBefore = rows("challenge_entry");
    String challenge = createChallenge(source);
    assertThat(createChallenge(source)).isEqualTo(challenge);

    String friend =
        objectMapper
            .readTree(body(post("/api/challenges/%s/games".formatted(challenge))))
            .get("id")
            .asString();
    answer(friend, 0, "CORRECT");
    assertThat(rows("challenge_entry")).isEqualTo(entriesBefore + 1);
    answer(friend, 1, "GAVE_UP");

    assertThat(rows("challenge_entry")).isEqualTo(entriesBefore + 2);
    assertThat(createChallenge(friend)).isEqualTo(challenge);
    assertThat(rows("challenge")).isEqualTo(challengesBefore + 1);
    assertThat(rows("challenge_entry")).isEqualTo(entriesBefore + 2);
    // The creator gave up both rounds, so the friend's score is now the one to beat.
    mockMvc
        .perform(get("/api/challenges/" + challenge))
        .andExpect(jsonPath("$.score").value(10))
        .andExpect(jsonPath("$.maxScore").value(20));
  }
}
