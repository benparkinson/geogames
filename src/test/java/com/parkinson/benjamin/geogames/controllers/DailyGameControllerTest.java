package com.parkinson.benjamin.geogames.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.api.DailyGameResponse;
import com.parkinson.benjamin.geogames.api.DailyScoreResponse;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.service.daily.DailyChallengeService;
import com.parkinson.benjamin.geogames.service.ratelimit.GameCapacityException;
import com.parkinson.benjamin.geogames.service.ratelimit.GameCreationGuard;
import com.parkinson.benjamin.geogames.service.ratelimit.TooManyGamesException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DailyGameController.class)
class DailyGameControllerTest {

  @Autowired private MockMvc mockMvc;

  private static final UUID GAME_ID = UUID.fromString("0b7c2a8e-4f1d-4c3a-9e5b-1d2f3a4b5c6d");

  @MockitoBean private DailyChallengeService dailyService;
  @MockitoBean private GameCreationGuard guard;

  @Test
  void aClientOverTheLimitGetsA429AndNoGameIsMade() throws Exception {
    doThrow(new TooManyGamesException(30)).when(guard).check(any());

    mockMvc
        .perform(post("/api/daily-games").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "30"));

    verifyNoInteractions(dailyService);
  }

  @Test
  void aFullDatabaseGetsA503() throws Exception {
    doThrow(new GameCapacityException(600)).when(guard).check(any());

    mockMvc
        .perform(post("/api/daily-games").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "600"));

    verifyNoInteractions(dailyService);
  }

  @Test
  void aDailyGameWithADateUsesThatDate() throws Exception {
    LocalDate date = LocalDate.of(2026, 11, 20);
    when(dailyService.createDailyGame(Optional.of(date), null))
        .thenReturn(new DailyGameResponse(GAME_ID, date, 3));

    mockMvc
        .perform(
            post("/api/daily-games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-11-20\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(GAME_ID.toString()))
        .andExpect(jsonPath("$.date").value("2026-11-20"))
        .andExpect(jsonPath("$.number").value(3));
  }

  @Test
  void hardModeIsPassedToTheService() throws Exception {
    LocalDate date = LocalDate.of(2026, 11, 20);
    when(dailyService.createDailyGame(Optional.empty(), Difficulty.HARD))
        .thenReturn(new DailyGameResponse(GAME_ID, date, 3));

    mockMvc
        .perform(
            post("/api/daily-games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hardMode\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(3));
  }

  @Test
  void aDailyGameWithNoDateUsesTheServersDate() throws Exception {
    LocalDate date = LocalDate.of(2026, 11, 20);
    when(dailyService.createDailyGame(Optional.empty(), null))
        .thenReturn(new DailyGameResponse(GAME_ID, date, 3));

    mockMvc
        .perform(post("/api/daily-games").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/daily-games")).andExpect(status().isOk());
  }

  @Test
  void theScoreEndpointReturnsTheDaysTotals() throws Exception {
    LocalDate date = LocalDate.of(2026, 11, 20);
    when(dailyService.score(date, Difficulty.NORMAL))
        .thenReturn(
            new DailyScoreResponse(date, "NORMAL", 0, null, 500, Collections.nCopies(11, 0)));

    mockMvc
        .perform(get("/api/daily-scores/2026-11-20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2026-11-20"))
        .andExpect(jsonPath("$.players").value(0))
        .andExpect(jsonPath("$.averageScore").doesNotExist())
        .andExpect(jsonPath("$.maxScore").value(500))
        .andExpect(jsonPath("$.buckets.length()").value(11));
  }

  @Test
  void aBadScoreDateIsABadRequest() throws Exception {
    mockMvc.perform(get("/api/daily-scores/yesterday")).andExpect(status().isBadRequest());

    when(dailyService.score(LocalDate.of(2001, 1, 1), Difficulty.NORMAL))
        .thenThrow(new IllegalArgumentException("too early"));
    mockMvc.perform(get("/api/daily-scores/2001-01-01")).andExpect(status().isBadRequest());
  }

  @Test
  void aMalformedDailyDateIsABadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/daily-games")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"20/11/2026\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(dailyService);
  }
}
