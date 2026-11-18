package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.api.DailyGameRequest;
import com.parkinson.benjamin.geogames.api.DailyGameResponse;
import com.parkinson.benjamin.geogames.api.GameCreationRequest;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.api.GameRound;
import com.parkinson.benjamin.geogames.api.GameRoundAnswerRequest;
import com.parkinson.benjamin.geogames.api.GameRoundGuessRequest;
import com.parkinson.benjamin.geogames.api.GameRoundHintRequest;
import com.parkinson.benjamin.geogames.api.RoundAnswerResponse;
import com.parkinson.benjamin.geogames.api.RoundGuessesResponse;
import com.parkinson.benjamin.geogames.api.RoundHintsResponse;
import com.parkinson.benjamin.geogames.service.GameService;
import com.parkinson.benjamin.geogames.service.daily.DailyChallengeService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GameController {

  private final GameService gameService;
  private final DailyChallengeService dailyChallengeService;

  public GameController(GameService gameService, DailyChallengeService dailyChallengeService) {
    this.gameService = gameService;
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
    return ResponseEntity.ok(dailyChallengeService.createDailyGame(date));
  }

  @PostMapping("/games")
  public ResponseEntity<GameCreationResponse> createGame(@RequestBody GameCreationRequest request) {

    return ResponseEntity.ok(gameService.createGame(request.gameType(), request.numberOfRounds()));
  }

  @GetMapping("/games/{gameId}/rounds/{round}")
  public ResponseEntity<GameRound> getGameRound(
      @PathVariable String gameId, @PathVariable int round) {
    return parseGameId(gameId)
        .flatMap(id -> gameService.getGameRound(id, round))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/games/{gameId}/rounds/{round}/answers")
  public ResponseEntity<RoundAnswerResponse> submitAnswer(
      @PathVariable String gameId,
      @PathVariable int round,
      @RequestBody GameRoundAnswerRequest answer) {
    return parseGameId(gameId)
        .flatMap(
            id ->
                retryOnConflict(
                    () ->
                        gameService.submitAnswer(
                            id,
                            round,
                            answer.answerState(),
                            answer.hintsUsedOrZero(),
                            answer.wrongGuesses())))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/games/{gameId}/rounds/{round}/guesses")
  public ResponseEntity<RoundGuessesResponse> recordGuess(
      @PathVariable String gameId,
      @PathVariable int round,
      @RequestBody GameRoundGuessRequest request) {
    if (request.correct() == null) {
      throw new IllegalArgumentException("correct is required");
    }
    return parseGameId(gameId)
        .flatMap(
            id ->
                retryOnConflict(
                    () -> gameService.recordGuess(id, round, request.guess(), request.correct())))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/games/{gameId}/rounds/{round}/hints")
  public ResponseEntity<RoundHintsResponse> recordHint(
      @PathVariable String gameId,
      @PathVariable int round,
      @RequestBody GameRoundHintRequest request) {
    if (request.hint() == null || request.guessesBefore() == null) {
      throw new IllegalArgumentException("hint and guessesBefore are required");
    }
    return parseGameId(gameId)
        .flatMap(
            id ->
                retryOnConflict(
                    () ->
                        gameService.recordHint(id, round, request.hint(), request.guessesBefore())))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Another request changed this round between this one's read and its write. Trying again reads
   * what that request committed, so the usual rules apply: the identical answer or guess is a
   * no-op, a different answer is a 409, and a game deleted meanwhile is a 404. It retries once,
   * outside the service's transaction; a second failure propagates.
   */
  private static <T> Optional<T> retryOnConflict(Supplier<Optional<T>> call) {
    try {
      return call.get();
    } catch (ObjectOptimisticLockingFailureException e) {
      return call.get();
    }
  }

  /**
   * A malformed id (including an old numeric one) is empty, so it is a 404 like an unknown game:
   * from the player's side it is a game that doesn't exist.
   */
  private static Optional<UUID> parseGameId(String gameId) {
    try {
      return Optional.of(UUID.fromString(gameId));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
