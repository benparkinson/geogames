package com.parkinson.benjamin.geogames.controllers;

import com.parkinson.benjamin.geogames.service.RoundAlreadyAnsweredException;
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

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail badRequest(IllegalArgumentException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(RoundAlreadyAnsweredException.class)
  public ProblemDetail conflict(RoundAlreadyAnsweredException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }
}
