package com.parkinson.benjamin.geogames.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.dao.GameType;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameCreationResponse;
import com.parkinson.benjamin.geogames.model.GameResult;
import com.parkinson.benjamin.geogames.model.GameRound;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.service.GameService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
class GameControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GameService gameService;

  private static GameRound round() {
    return new GameRound(
        GameType.TRIPOINT,
        "{\"countries\":[]}",
        3,
        AnswerState.UNANSWERED,
        new GameResult(
            GameState.CREATED,
            3,
            0,
            List.of(AnswerState.UNANSWERED, AnswerState.UNANSWERED, AnswerState.UNANSWERED)));
  }

  // ------------------------------------------------------------ create game

  @Test
  void createsAGame() throws Exception {
    when(gameService.createGame("TRIPOINT", 3))
        .thenReturn(Optional.of(new GameCreationResponse(42)));

    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(42));

    verify(gameService).createGame("TRIPOINT", 3);
  }

  @Test
  void createGameReturnsBadRequestWhenServiceReturnsNothing() throws Exception {
    when(gameService.createGame("TRIPOINT", 3)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":3}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createGameWithInvalidArgumentsFailsInsteadOfReturningBadRequest() throws Exception {
    when(gameService.createGame("FOO", 3)).thenThrow(new IllegalArgumentException("Unknown"));

    // TODO: there is no exception handler, so the IllegalArgumentException from the
    // service escapes the controller (a 500 in a real server) instead of becoming a 400.
    assertThatThrownBy(
            () ->
                mockMvc.perform(
                    post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameType\":\"FOO\",\"numberOfRounds\":3}")))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
  }

  // -------------------------------------------------------------- get round

  @Test
  void returnsTheRoundContractTheFrontendRelieson() throws Exception {
    when(gameService.getGameRound(1L, 0)).thenReturn(Optional.of(round()));

    mockMvc
        .perform(get("/api/games/1/rounds/0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameType.name").value("TRIPOINT"))
        .andExpect(jsonPath("$.gameType.answerCount").value(3))
        .andExpect(jsonPath("$.jsonBlob").value("{\"countries\":[]}"))
        .andExpect(jsonPath("$.totalRoundCount").value(3))
        .andExpect(jsonPath("$.answerState").value("UNANSWERED"))
        .andExpect(jsonPath("$.gameResult.gameState").value("CREATED"))
        .andExpect(jsonPath("$.gameResult.score").value(0))
        .andExpect(jsonPath("$.gameResult.roundsUnanswered").value(3))
        .andExpect(jsonPath("$.gameResult.roundAnswerStates.length()").value(3))
        .andExpect(jsonPath("$.gameResult.roundAnswerStates[0]").value("UNANSWERED"));
  }

  @Test
  void unknownRoundIsNotFound() throws Exception {
    when(gameService.getGameRound(1L, 7)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/games/1/rounds/7")).andExpect(status().isNotFound());
  }

  @Test
  void nonNumericGameIdIsABadRequest() throws Exception {
    mockMvc.perform(get("/api/games/abc/rounds/0")).andExpect(status().isBadRequest());
  }

  // ----------------------------------------------------------- submit answer

  @Test
  void submitsAnAnswer() throws Exception {
    when(gameService.submitAnswer(1L, 0, AnswerState.CORRECT)).thenReturn(Optional.of(round()));

    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameType.name").value("TRIPOINT"));

    verify(gameService).submitAnswer(1L, 0, AnswerState.CORRECT);
  }

  @Test
  void answeringAnUnknownRoundIsNotFound() throws Exception {
    when(gameService.submitAnswer(1L, 9, AnswerState.GAVE_UP)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/games/1/rounds/9/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"GAVE_UP\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void invalidAnswerStateIsABadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"WRONG\"}"))
        .andExpect(status().isBadRequest());
  }

  // -------------------------------------------------------------------- CORS

  @Test
  void allowsTheLocalFrontendOrigin() throws Exception {
    when(gameService.getGameRound(1L, 0)).thenReturn(Optional.of(round()));

    mockMvc
        .perform(get("/api/games/1/rounds/0").header("Origin", "http://localhost:3000"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
  }

  @Test
  void allowsTheDeployedFrontendOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/games")
                .header("Origin", "https://geogauntlet.onrender.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Access-Control-Allow-Origin", "https://geogauntlet.onrender.com"));
  }

  @Test
  void rejectsOtherOrigins() throws Exception {
    when(gameService.getGameRound(1L, 0)).thenReturn(Optional.of(round()));

    var actual =
        mockMvc
            .perform(get("/api/games/1/rounds/0").header("Origin", "https://evil.example"))
            .andExpect(status().isForbidden())
            .andReturn();
    assertThat(actual.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();

    var preflight =
        mockMvc
            .perform(
                options("/api/games")
                    .header("Origin", "https://evil.example")
                    .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden())
            .andReturn();
    assertThat(preflight.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
  }
}
