package com.parkinson.benjamin.geogames.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.api.ChallengeCreationResponse;
import com.parkinson.benjamin.geogames.api.ChallengeResponse;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeConflictException;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeNotFoundException;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeService;
import com.parkinson.benjamin.geogames.service.ratelimit.GameCreationGuard;
import com.parkinson.benjamin.geogames.service.ratelimit.TooManyGamesException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChallengeController.class)
class ChallengeControllerTest {

  private static final UUID ID = UUID.fromString("0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ChallengeService challenges;
  @MockitoBean private GameCreationGuard guard;

  @Test
  void createReturnsTheChallengeId() throws Exception {
    UUID challenge = UUID.randomUUID();
    when(challenges.create(ID)).thenReturn(new ChallengeCreationResponse(challenge));

    mockMvc
        .perform(post("/api/games/" + ID + "/challenges"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(challenge.toString()));

    verify(guard).check(any());
  }

  @Test
  void createMapsTheServiceErrors() throws Exception {
    UUID unknown = UUID.randomUUID();
    UUID unfinished = UUID.randomUUID();
    UUID daily = UUID.randomUUID();
    when(challenges.create(unknown)).thenThrow(new ChallengeNotFoundException("No such game"));
    when(challenges.create(unfinished)).thenThrow(new ChallengeConflictException("open"));
    when(challenges.create(daily)).thenThrow(new BadRequestException("daily"));

    mockMvc.perform(post("/api/games/" + unknown + "/challenges")).andExpect(status().isNotFound());
    mockMvc
        .perform(post("/api/games/" + unfinished + "/challenges"))
        .andExpect(status().isConflict());
    mockMvc.perform(post("/api/games/" + daily + "/challenges")).andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/games/nope/challenges")).andExpect(status().isBadRequest());
  }

  @Test
  void findReturnsTheChallengeWithoutRounds() throws Exception {
    when(challenges.find(ID)).thenReturn(new ChallengeResponse("RANDOM", "EASY", 5, 300, 500));

    mockMvc
        .perform(get("/api/challenges/" + ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameMode").value("RANDOM"))
        .andExpect(jsonPath("$.roundCount").value(5))
        .andExpect(jsonPath("$.score").value(300));
    mockMvc.perform(get("/api/challenges/nope")).andExpect(status().isBadRequest());
  }

  @Test
  void anExpiredChallengeIsNotFound() throws Exception {
    when(challenges.find(ID))
        .thenThrow(new ChallengeNotFoundException("This challenge has expired"));

    mockMvc
        .perform(get("/api/challenges/" + ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("This challenge has expired"));
  }

  @Test
  void createGameReturnsANewGameId() throws Exception {
    UUID game = UUID.randomUUID();
    when(challenges.createGame(ID)).thenReturn(new GameCreationResponse(game));

    mockMvc
        .perform(post("/api/challenges/" + ID + "/games"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(game.toString()));
    mockMvc.perform(post("/api/challenges/nope/games")).andExpect(status().isBadRequest());
  }

  @Test
  void aClientOverTheLimitGetsA429OnBothPosts() throws Exception {
    doThrow(new TooManyGamesException(30)).when(guard).check(any());

    mockMvc
        .perform(post("/api/games/" + ID + "/challenges"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "30"));
    mockMvc
        .perform(post("/api/challenges/" + ID + "/games"))
        .andExpect(status().isTooManyRequests());

    verifyNoInteractions(challenges);
  }
}
