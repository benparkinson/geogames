package com.parkinson.benjamin.geogames.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.parkinson.benjamin.geogames.api.RecordedGuess;
import com.parkinson.benjamin.geogames.api.RoundAnswerResponse;
import com.parkinson.benjamin.geogames.api.RoundGuessesResponse;
import com.parkinson.benjamin.geogames.api.RoundHintsResponse;
import com.parkinson.benjamin.geogames.dao.GameRoundEntity;
import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameState;
import com.parkinson.benjamin.geogames.model.GameType;
import com.parkinson.benjamin.geogames.service.GameService;
import com.parkinson.benjamin.geogames.service.RoundAlreadyAnsweredException;
import com.parkinson.benjamin.geogames.service.RoundClosedException;
import com.parkinson.benjamin.geogames.service.RoundDataMissingException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(GameController.class)
class GameControllerTest {

  @Autowired private MockMvc mockMvc;

  private static final UUID GAME_ID = UUID.fromString("0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d");

  @MockitoBean private GameService gameService;

  private static RoundAnswerResponse answer() {
    return new RoundAnswerResponse(AnswerState.CORRECT, 0, 100, round().gameResult());
  }

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
            List.of(0, 0, 0)),
        List.of(),
        List.of(),
        false);
  }

  // ------------------------------------------------------------ create game

  @Test
  void createsAGame() throws Exception {
    when(gameService.createGame("TRIPOINT", 3, null)).thenReturn(new GameCreationResponse(GAME_ID));

    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(GAME_ID.toString()));

    verify(gameService).createGame("TRIPOINT", 3, null);
  }

  @Test
  void hardModeIsPassedToTheService() throws Exception {
    when(gameService.createGame("TRIPOINT", 3, true)).thenReturn(new GameCreationResponse(GAME_ID));

    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameType\":\"TRIPOINT\",\"numberOfRounds\":3,\"hardMode\":true}"))
        .andExpect(status().isOk());

    verify(gameService).createGame("TRIPOINT", 3, true);
  }

  @Test
  void createGameWithInvalidArgumentsIsABadRequestWithAProblemBody() throws Exception {
    when(gameService.createGame("FOO", 3, null)).thenThrow(new IllegalArgumentException("Unknown"));

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
    when(gameService.getGameRound(GAME_ID, 0)).thenReturn(Optional.of(round()));

    mockMvc
        .perform(get("/api/games/" + GAME_ID + "/rounds/0"))
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
    when(gameService.getGameRound(GAME_ID, 0))
        .thenThrow(new RoundDataMissingException(GameType.TRIPOINT, "tripoint:gone"));

    mockMvc
        .perform(get("/api/games/" + GAME_ID + "/rounds/0"))
        .andExpect(status().isGone())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(410))
        .andExpect(
            jsonPath("$.detail")
                .value(org.hamcrest.Matchers.containsString("no longer available")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "42", "0b7c2a8e-4f1d-4c3a-9e5b"})
  void aMalformedOrNumericGameIdIsNotFoundForTheRoundAndTheAnswer(String id) throws Exception {
    mockMvc.perform(get("/api/games/" + id + "/rounds/0")).andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/games/" + id + "/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"GAVE_UP\"}"))
        .andExpect(status().isNotFound());

    verifyNoInteractions(gameService);
  }

  @Test
  void unknownRoundIsNotFound() throws Exception {
    when(gameService.getGameRound(GAME_ID, 7)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/games/" + GAME_ID + "/rounds/7")).andExpect(status().isNotFound());
  }

  @Test
  void nonNumericRoundIsABadRequest() throws Exception {
    mockMvc
        .perform(get("/api/games/" + GAME_ID + "/rounds/abc"))
        .andExpect(status().isBadRequest());
  }

  // ----------------------------------------------------------- submit answer

  @Test
  void submitsAnAnswer() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null))
        .thenReturn(Optional.of(answer()));

    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answerState").value("CORRECT"))
        .andExpect(jsonPath("$.points").value(100))
        .andExpect(jsonPath("$.gameResult.maxScore").value(300))
        .andExpect(jsonPath("$.jsonBlob").doesNotExist());

    verify(gameService).submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 0, null);
  }

  @Test
  void anAnswerWithHintsUsedPassesThemOn() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 2, null))
        .thenReturn(Optional.of(answer()));

    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":2}"))
        .andExpect(status().isOk());

    verify(gameService).submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 2, null);
  }

  @Test
  void anAnswerWithTooManyHintsIsABadRequestWithAProblemBody() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 9, null))
        .thenThrow(
            new IllegalArgumentException(
                "A round of TRIPOINT has 3 hints, so 9 can't have been used"));

    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":9}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("A round of TRIPOINT has 3 hints, so 9 can't have been used"));
  }

  @Test
  void answeringAnUnknownRoundIsNotFound() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 9, AnswerState.GAVE_UP, 0, null))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/9/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"GAVE_UP\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void anAnswerTheServiceRejectsIsABadRequest() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.UNANSWERED, 0, null))
        .thenThrow(new IllegalArgumentException("A round can't be answered with UNANSWERED"));

    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"UNANSWERED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("A round can't be answered with UNANSWERED"));
  }

  @Test
  void changingAnAnsweredRoundIsAConflict() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null))
        .thenThrow(
            new RoundAlreadyAnsweredException(
                0, AnswerState.CORRECT, 0, 2, AnswerState.GAVE_UP, 0, null));

    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/0/answers")
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

  private static ObjectOptimisticLockingFailureException lockFailure() {
    return new ObjectOptimisticLockingFailureException(GameRoundEntity.class, 1L);
  }

  private ResultActions postGaveUp() throws Exception {
    return mockMvc.perform(
        post("/api/games/" + GAME_ID + "/rounds/0/answers")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"answerState\":\"GAVE_UP\"}"));
  }

  @Test
  void anAnswerThatLostARaceIsRetriedOnce() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null))
        .thenThrow(lockFailure())
        .thenReturn(Optional.of(answer()));

    postGaveUp().andExpect(status().isOk());

    verify(gameService, times(2)).submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null);
  }

  @Test
  void anAnswerThatLostARaceToADifferentAnswerIsAConflict() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null))
        .thenThrow(lockFailure())
        .thenThrow(
            new RoundAlreadyAnsweredException(
                0, AnswerState.CORRECT, 0, 2, AnswerState.GAVE_UP, 0, null));

    postGaveUp().andExpect(status().isConflict());
  }

  @Test
  void anAnswerThatLosesTheRaceTwiceIsAConflictAndIsNotRetriedAgain() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null))
        .thenThrow(lockFailure());

    postGaveUp()
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value("This round was changed by another request. Please try again."));

    verify(gameService, times(2)).submitAnswer(GAME_ID, 0, AnswerState.GAVE_UP, 0, null);
  }

  @Test
  void anAnswerWithWrongGuessesPassesThemOn() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 1, 3))
        .thenReturn(Optional.of(answer()));

    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":1,\"wrongGuesses\":3}"))
        .andExpect(status().isOk());

    verify(gameService).submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 1, 3);
  }

  @Test
  void anAnswerWithoutWrongGuessesPassesNull() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 1, null))
        .thenReturn(Optional.of(answer()));

    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"CORRECT\",\"hintsUsed\":1}"))
        .andExpect(status().isOk());

    verify(gameService).submitAnswer(GAME_ID, 0, AnswerState.CORRECT, 1, null);
  }

  @Test
  void anAnswerWithImpossibleWrongGuessesIsABadRequest() throws Exception {
    when(gameService.submitAnswer(GAME_ID, 0, AnswerState.OUT_OF_GUESSES, 0, 3))
        .thenThrow(
            new IllegalArgumentException(
                "A round of TRIPOINT that ran out of guesses must have 5 wrong guesses, not 3"));

    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"OUT_OF_GUESSES\",\"wrongGuesses\":3}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "A round of TRIPOINT that ran out of guesses must have 5 wrong guesses, not"
                        + " 3"));
  }

  @Test
  void invalidAnswerStateIsABadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/games/" + GAME_ID + "/rounds/0/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answerState\":\"WRONG\"}"))
        .andExpect(status().isBadRequest());
  }

  // -------------------------------------------------------------------- CORS

  @Test
  void allowsTheLocalFrontendOrigin() throws Exception {
    when(gameService.getGameRound(GAME_ID, 0)).thenReturn(Optional.of(round()));

    mockMvc
        .perform(
            get("/api/games/" + GAME_ID + "/rounds/0").header("Origin", "http://localhost:3000"))
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
    when(gameService.getGameRound(GAME_ID, 0)).thenReturn(Optional.of(round()));

    var actual =
        mockMvc
            .perform(
                get("/api/games/" + GAME_ID + "/rounds/0").header("Origin", "https://evil.example"))
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

  // ---------------------------------------------------------------- guesses

  private ResultActions postGuess(String id, String body) throws Exception {
    return mockMvc.perform(
        post("/api/games/" + id + "/rounds/0/guesses")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private static final String WRONG_FRANCE = "{\"guess\":\"France\",\"correct\":false}";

  @Test
  void recordsAGuess() throws Exception {
    when(gameService.recordGuess(GAME_ID, 0, "France", false))
        .thenReturn(
            Optional.of(new RoundGuessesResponse(List.of(new RecordedGuess("France", false)))));

    postGuess(GAME_ID.toString(), WRONG_FRANCE)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.guesses[0].text").value("France"))
        .andExpect(jsonPath("$.guesses[0].correct").value(false));
  }

  @Test
  void aGuessForAMalformedOrUnknownGameIsNotFound() throws Exception {
    when(gameService.recordGuess(GAME_ID, 0, "France", false)).thenReturn(Optional.empty());

    postGuess("12", WRONG_FRANCE).andExpect(status().isNotFound());
    postGuess(GAME_ID.toString(), WRONG_FRANCE).andExpect(status().isNotFound());
  }

  @Test
  void aGuessWithoutCorrectIsABadRequest() throws Exception {
    postGuess(GAME_ID.toString(), "{\"guess\":\"France\"}").andExpect(status().isBadRequest());

    verify(gameService, never()).recordGuess(any(), anyInt(), any(), anyBoolean());
  }

  @Test
  void aGuessOnAnAnsweredRoundIsAConflict() throws Exception {
    when(gameService.recordGuess(GAME_ID, 0, "France", false))
        .thenThrow(new RoundClosedException(0));

    postGuess(GAME_ID.toString(), WRONG_FRANCE).andExpect(status().isConflict());
  }

  @Test
  void aGuessThatLostARaceIsRetriedOnce() throws Exception {
    when(gameService.recordGuess(GAME_ID, 0, "France", false))
        .thenThrow(lockFailure())
        .thenReturn(Optional.of(new RoundGuessesResponse(List.of())));

    postGuess(GAME_ID.toString(), WRONG_FRANCE).andExpect(status().isOk());

    verify(gameService, times(2)).recordGuess(GAME_ID, 0, "France", false);
  }

  // ------------------------------------------------------------------ hints

  private ResultActions postHint(String id, String body) throws Exception {
    return mockMvc.perform(
        post("/api/games/" + id + "/rounds/0/hints")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private static final String FIRST_HINT = "{\"hint\":0,\"guessesBefore\":2}";

  @Test
  void recordsAHint() throws Exception {
    when(gameService.recordHint(GAME_ID, 0, 0, 2))
        .thenReturn(Optional.of(new RoundHintsResponse(List.of(2))));

    postHint(GAME_ID.toString(), FIRST_HINT)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hintsTakenAt[0]").value(2));
  }

  @Test
  void aHintForAMalformedOrUnknownGameIsNotFound() throws Exception {
    when(gameService.recordHint(GAME_ID, 0, 0, 2)).thenReturn(Optional.empty());

    postHint("12", FIRST_HINT).andExpect(status().isNotFound());
    postHint(GAME_ID.toString(), FIRST_HINT).andExpect(status().isNotFound());
  }

  @Test
  void aHintWithAMissingFieldIsABadRequest() throws Exception {
    postHint(GAME_ID.toString(), "{\"guessesBefore\":2}").andExpect(status().isBadRequest());
    postHint(GAME_ID.toString(), "{\"hint\":0}").andExpect(status().isBadRequest());

    verify(gameService, never()).recordHint(any(), anyInt(), anyInt(), anyInt());
  }

  @Test
  void aHintOnAnAnsweredRoundIsAConflict() throws Exception {
    when(gameService.recordHint(GAME_ID, 0, 0, 2)).thenThrow(new RoundClosedException(0));

    postHint(GAME_ID.toString(), FIRST_HINT).andExpect(status().isConflict());
  }

  @Test
  void aHintThatLostARaceIsRetriedOnce() throws Exception {
    when(gameService.recordHint(GAME_ID, 0, 0, 2))
        .thenThrow(lockFailure())
        .thenReturn(Optional.of(new RoundHintsResponse(List.of(2))));

    postHint(GAME_ID.toString(), FIRST_HINT).andExpect(status().isOk());

    verify(gameService, times(2)).recordHint(GAME_ID, 0, 0, 2);
  }
}
