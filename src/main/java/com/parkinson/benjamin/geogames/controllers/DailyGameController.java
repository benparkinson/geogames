package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.api.DailyGameRequest;
import com.parkinson.benjamin.geogames.api.DailyGameResponse;
import com.parkinson.benjamin.geogames.api.DailyScoreResponse;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.service.daily.DailyChallengeService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
      date = Optional.of(parseDate(request.date()));
    }
    Difficulty difficulty =
        request == null ? null : Difficulty.resolve(request.difficulty(), request.hardMode());
    return ResponseEntity.ok(dailyChallengeService.createDailyGame(date, difficulty));
  }

  /** One day's totals: the average, and how many finished in each tenth of the score range. */
  @GetMapping("/daily-scores/{date}")
  public DailyScoreResponse dailyScore(
      @PathVariable String date, @RequestParam(required = false) String difficulty) {
    return dailyChallengeService.score(
        parseDate(date),
        difficulty == null ? Difficulty.NORMAL : Difficulty.resolve(difficulty, null));
  }

  private static LocalDate parseDate(String text) {
    try {
      return LocalDate.parse(text);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("date must be YYYY-MM-DD");
    }
  }
}
