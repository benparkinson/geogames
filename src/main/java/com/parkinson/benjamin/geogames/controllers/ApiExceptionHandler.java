package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.service.RoundAlreadyAnsweredException;
import com.parkinson.benjamin.geogames.service.RoundDataMissingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns bad input into a 400, and a request that conflicts with the game's state (such as changing
 * an answered round) into a 409, both with a JSON problem body. Anything else keeps Spring's
 * default 500.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail badRequest(IllegalArgumentException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(RoundAlreadyAnsweredException.class)
  public ProblemDetail conflict(RoundAlreadyAnsweredException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
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
