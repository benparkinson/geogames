package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.api.DailyGameRequest;
import com.parkinson.benjamin.geogames.api.DailyGameResponse;
import com.parkinson.benjamin.geogames.service.daily.DailyChallengeService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DailyGameController {

  private final DailyChallengeService dailyChallengeService;

  public DailyGameController(DailyChallengeService dailyChallengeService) {
    this.dailyChallengeService = dailyChallengeService;
  }

  /** A new game from the daily challenge's pinned rounds. The body may be {@code {}}. */
  @PostMapping("/daily-games")
  public ResponseEntity<DailyGameResponse> createDailyGame(
      @RequestBody(required = false) DailyGameRequest request) {
    Optional<LocalDate> date = Optional.empty();
    if (request != null && request.date() != null) {
      try {
        date = Optional.of(LocalDate.parse(request.date()));
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("date must be YYYY-MM-DD");
      }
    }
    return ResponseEntity.ok(
        dailyChallengeService.createDailyGame(date, request == null ? null : request.hardMode()));
  }
}
