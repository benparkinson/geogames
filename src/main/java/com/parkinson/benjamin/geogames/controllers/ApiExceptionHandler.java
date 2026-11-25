package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.api.BadRequestException;
import com.parkinson.benjamin.geogames.service.RoundAlreadyAnsweredException;
import com.parkinson.benjamin.geogames.service.RoundClosedException;
import com.parkinson.benjamin.geogames.service.RoundDataMissingException;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeConflictException;
import com.parkinson.benjamin.geogames.service.challenge.ChallengeNotFoundException;
import com.parkinson.benjamin.geogames.service.ratelimit.GameCapacityException;
import com.parkinson.benjamin.geogames.service.ratelimit.TooManyGamesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions into JSON problem bodies: bad input (a {@link BadRequestException}) is a 400, a conflict with the game's state
 * (such as changing an answered round, or a round changed by another request at the same moment) is
 * a 409, and round data that has gone is a 410. Anything else keeps Spring's default 500.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(BadRequestException.class)
  public ProblemDetail badRequest(BadRequestException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(ChallengeNotFoundException.class)
  public ProblemDetail notFound(ChallengeNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler({
    RoundAlreadyAnsweredException.class,
    RoundClosedException.class,
    ChallengeConflictException.class
  })
  public ProblemDetail conflict(RuntimeException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  /** The message names the entity and its id, so it is neither returned nor logged. */
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ProblemDetail changedElsewhere() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "This round was changed by another request. Please try again.");
  }

  @ExceptionHandler(TooManyGamesException.class)
  public ResponseEntity<ProblemDetail> tooManyGames(TooManyGamesException e) {
    return retryLater(
        HttpStatus.TOO_MANY_REQUESTS,
        "Too many new games from your network. Try again in a few minutes.",
        e.retryAfterSeconds());
  }

  @ExceptionHandler(GameCapacityException.class)
  public ResponseEntity<ProblemDetail> atCapacity(GameCapacityException e) {
    return retryLater(
        HttpStatus.SERVICE_UNAVAILABLE,
        "The game server is busy. Try again later.",
        e.retryAfterSeconds());
  }

  private static ResponseEntity<ProblemDetail> retryLater(
      HttpStatus status, String detail, long retryAfterSeconds) {
    return ResponseEntity.status(status)
        .header("Retry-After", Long.toString(retryAfterSeconds))
        .body(ProblemDetail.forStatusAndDetail(status, detail));
  }

  @ExceptionHandler(RoundDataMissingException.class)
  public ProblemDetail gone(RoundDataMissingException e) {
    log.warn("Round data is gone: {}", e.getMessage());
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.GONE,
        "This game was created with map data that is no longer available. Please start a new"
            + " game.");
  }
}
