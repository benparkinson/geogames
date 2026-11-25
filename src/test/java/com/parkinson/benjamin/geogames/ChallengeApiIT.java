package com.parkinson.benjamin.geogames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.service.challenge.Challenge;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeStore;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "geogames.rate-limit.games-per-window=1000")
class ChallengeApiIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ChallengeStore store;

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

  @Test
  void theStoreInsertsFindsAndDeletesByDate() {
    UUID id = UUID.randomUUID();
    Challenge old =
        new Challenge(
            id,
            "TRIPOINT",
            com.parkinson.benjamin.geogames.model.Difficulty.EASY,
            java.util.List.of(
                new com.parkinson.benjamin.geogames.service.daily.PinnedRound(
                    com.parkinson.benjamin.geogames.model.GameType.TRIPOINT, "tripoint:x")),
            10,
            100,
            LocalDate.of(2000, 1, 1));
    store.insert(old);

    assertThat(store.find(id)).contains(old);
    assertThat(store.deleteCreatedBefore(LocalDate.of(2000, 1, 2))).isGreaterThanOrEqualTo(1);
    assertThat(store.find(id)).isEmpty();
  }
}
