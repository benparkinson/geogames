package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.api.GameCreationRequest;
import com.parkinson.benjamin.geogames.api.GameCreationResponse;
import com.parkinson.benjamin.geogames.api.GameRound;
import com.parkinson.benjamin.geogames.api.GameRoundAnswerRequest;
import com.parkinson.benjamin.geogames.api.GameRoundGuessRequest;
import com.parkinson.benjamin.geogames.api.GameRoundHintRequest;
import com.parkinson.benjamin.geogames.api.RoundAnswerResponse;
import com.parkinson.benjamin.geogames.api.RoundGuessesResponse;
import com.parkinson.benjamin.geogames.api.RoundHintsResponse;
import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.service.GameService;
import com.parkinson.benjamin.geogames.service.ratelimit.GameCreationGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
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

  private final GameCreationGuard guard;

  public GameController(GameService gameService, GameCreationGuard guard) {
    this.gameService = gameService;
    this.guard = guard;
  }

  @PostMapping("/games")
  public ResponseEntity<GameCreationResponse> createGame(
      @RequestBody GameCreationRequest request, HttpServletRequest httpRequest) {
    guard.check(httpRequest);
    return ResponseEntity.ok(
        gameService.createGame(
            request.gameType(),
            request.numberOfRounds(),
            Difficulty.resolve(request.difficulty())));
  }

  @GetMapping("/games/{gameId}/rounds/{round}")
  public ResponseEntity<GameRound> getGameRound(
      @PathVariable String gameId, @PathVariable int round) {
    return forGame(gameId, id -> gameService.getGameRound(id, round));
  }

  @PostMapping("/games/{gameId}/rounds/{round}/answers")
  public ResponseEntity<RoundAnswerResponse> submitAnswer(
      @PathVariable String gameId,
      @PathVariable int round,
      @RequestBody GameRoundAnswerRequest answer) {
    return forGame(
        gameId,
        id ->
            retryOnConflict(
                () ->
                    gameService.submitAnswer(
                        id,
                        round,
                        answer.answerState(),
                        answer.hintsUsedOrZero(),
                        answer.wrongGuesses(),
                        answer.answersFound())));
  }

  @PostMapping("/games/{gameId}/rounds/{round}/guesses")
  public ResponseEntity<RoundGuessesResponse> recordGuess(
      @PathVariable String gameId,
      @PathVariable int round,
      @RequestBody GameRoundGuessRequest request) {
    if (request.correct() == null) {
      throw new BadRequestException("correct is required");
    }
    return forGame(
        gameId,
        id ->
            retryOnConflict(
                () -> gameService.recordGuess(id, round, request.guess(), request.correct())));
  }

  @PostMapping("/games/{gameId}/rounds/{round}/hints")
  public ResponseEntity<RoundHintsResponse> recordHint(
      @PathVariable String gameId,
      @PathVariable int round,
      @RequestBody GameRoundHintRequest request) {
    if (request.hint() == null || request.guessesBefore() == null) {
      throw new BadRequestException("hint and guessesBefore are required");
    }
    return forGame(
        gameId,
        id ->
            retryOnConflict(
                () -> gameService.recordHint(id, round, request.hint(), request.guessesBefore())));
  }

  /** A 404 when the id is malformed or the action finds no game or round, otherwise a 200. */
  private static <T> ResponseEntity<T> forGame(String gameId, Function<UUID, Optional<T>> action) {
    return ResponseEntity.of(parseGameId(gameId).flatMap(action));
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
