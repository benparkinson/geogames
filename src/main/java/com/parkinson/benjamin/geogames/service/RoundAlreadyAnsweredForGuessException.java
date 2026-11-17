package com.parkinson.benjamin.geogames.service;

/** Thrown when a guess is sent to a round that already has an answer. */
public class RoundAlreadyAnsweredForGuessException extends RuntimeException {

  public RoundAlreadyAnsweredForGuessException(int roundNumber) {
    super("Round %d is already answered, so it can't take another guess".formatted(roundNumber));
  }
}
