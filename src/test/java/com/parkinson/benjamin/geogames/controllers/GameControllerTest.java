package com.parkinson.benjamin.geogames.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.api.GameResult;
import com.parkinson.benjamin.geogames.api.GameRound;
import com.parkinson.benjamin.geogames.api.GameTypeResponse;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.GameService;
import com.parkinson.benjamin.geogames.service.RoundAlreadyAnsweredException;
import com.parkinson.benjamin.geogames.service.RoundDataMissingException;
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
        GameTypeResponse.from(GameType.TRIPOINT),
        "{\"countries\":[]}",
        3,
        AnswerState.UNANSWERED,
        0,
        0,
        new GameResult(
            GameState.CREATED,
            3,
            0,
            List.of(AnswerState.UNANSWERED, AnswerState.UNANSWERED, AnswerState.UNANSWERED),
            300,
            0,
            0,
            List.of(0, 0, 0),
            List.of(0, 0, 0)));
  }

  // ------------------------------------------------------------ create game

  @Test
  void createsAGame() throws Exception {
    when(gameService.createGame("TRIPOINT", 3)).thenReturn(new GameCreationResponse(42));

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
  void createGameWithInvalidArgumentsIsABadRequestWithAProblemBody() throws Exception {
    when(gameService.createGame("FOO", 3)).thenThrow(new IllegalArgumentException("Unknown"));

    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameType\":\"FOO\",\"numberOfRounds\":3}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Unknown"));
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
        .andExpect(jsonPath("$.gameResult.roundAnswerStates[0]").value("UNANSWERED"))
        .andExpect(jsonPath("$.gameType.hintCount").value(3))
        .andExpect(jsonPath("$.gameType.guessLimit").value(5))
        .andExpect(jsonPath("$.hintsUsed").value(0))
        .andExpect(jsonPath("$.points").value(0))
        .andExpect(jsonPath("$.gameResult.maxScore").value(300))
        .andExpect(jsonPath("$.gameResult.roundsCorrect").value(0))
        .andExpect(jsonPath("$.gameResult.perfectRounds").value(0))
        .andExpect(jsonPath("$.gameResult.roundPoints.length()").value(3))
        .andExpect(jsonPath("$.gameResult.roundHintsUsed.length()").value(3));
  }

  @Test
  void roundWhoseMapDataHasGoneIs410WithAProblemBody() throws Exception {
    when(gameService.getGameRound(1L, 0))
        .thenThrow(new RoundDataMissingException(GameType.TRIPOINT, "tripoint:gone"));

    mockMvc
        .perform(get("/api/games/1/rounds/0"))
        .andExpect(status().isGone())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(410))
        .andExpect(
            jsonPath("$.detail")
                .value(org.hamcrest.Matchers.containsString("no longer available")));
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
    when(gameService.submitAnswer(1L, 0, AnswerState.CORRECT, 0, null))
        .thenReturn(Optional.of(round()));

    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameType.name").value("TRIPOINT"));

    verify(gameService).submitAnswer(1L, 0, AnswerState.CORRECT, 0, null);
  }

  @Test
  void anAnswerWithHintsUsedPassesThemOn() throws Exception {
    when(gameService.submitAnswer(1L, 0, AnswerState.CORRECT, 2, null))
        .thenReturn(Optional.of(round()));

    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":2}"))
        .andExpect(status().isOk());

    verify(gameService).submitAnswer(1L, 0, AnswerState.CORRECT, 2, null);
  }

  @Test
  void anAnswerWithTooManyHintsIsABadRequestWithAProblemBody() throws Exception {
    when(gameService.submitAnswer(1L, 0, AnswerState.CORRECT, 9, null))
        .thenThrow(
            new IllegalArgumentException(
                "A round of TRIPOINT has 3 hints, so 9 can't have been used"));

    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":9}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("A round of TRIPOINT has 3 hints, so 9 can't have been used"));
  }

  @Test
  void answeringAnUnknownRoundIsNotFound() throws Exception {
    when(gameService.submitAnswer(1L, 9, AnswerState.GAVE_UP, 0, null))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/games/1/rounds/9/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"GAVE_UP\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void anAnswerTheServiceRejectsIsABadRequest() throws Exception {
    when(gameService.submitAnswer(1L, 0, AnswerState.UNANSWERED, 0, null))
        .thenThrow(new IllegalArgumentException("A round can't be answered with UNANSWERED"));

    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"UNANSWERED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("A round can't be answered with UNANSWERED"));
  }

  @Test
  void changingAnAnsweredRoundIsAConflict() throws Exception {
    when(gameService.submitAnswer(1L, 0, AnswerState.GAVE_UP, 0, null))
        .thenThrow(
            new RoundAlreadyAnsweredException(
                0, AnswerState.CORRECT, 0, 2, AnswerState.GAVE_UP, 0, null));

    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"GAVE_UP\"}"))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "Round 0 was already answered CORRECT with 0 hints and 2 wrong guesses and"
                        + " can't be changed to GAVE_UP with 0 hints and unreported wrong"
                        + " guesses"));
  }

  @Test
  void anAnswerWithWrongGuessesPassesThemOn() throws Exception {
    when(gameService.submitAnswer(1L, 0, AnswerState.CORRECT, 1, 3))
        .thenReturn(Optional.of(round()));

    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":1,\"wrongGuesses\":3}"))
        .andExpect(status().isOk());

    verify(gameService).submitAnswer(1L, 0, AnswerState.CORRECT, 1, 3);
  }

  @Test
  void anAnswerWithoutWrongGuessesPassesNull() throws Exception {
    when(gameService.submitAnswer(1L, 0, AnswerState.CORRECT, 1, null))
        .thenReturn(Optional.of(round()));

    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":1}"))
        .andExpect(status().isOk());

    verify(gameService).submitAnswer(1L, 0, AnswerState.CORRECT, 1, null);
  }

  @Test
  void anAnswerWithImpossibleWrongGuessesIsABadRequest() throws Exception {
    when(gameService.submitAnswer(1L, 0, AnswerState.OUT_OF_GUESSES, 0, 3))
        .thenThrow(
            new IllegalArgumentException(
                "A round of TRIPOINT that ran out of guesses had 5 wrong guesses, not 3"));

    mockMvc
        .perform(
            post("/api/games/1/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"OUT_OF_GUESSES\",\"wrongGuesses\":3}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("A round of TRIPOINT that ran out of guesses had 5 wrong guesses, not 3"));
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
