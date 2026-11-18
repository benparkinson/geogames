package com.parkinson.benjamin.geogames.service;

/** Thrown when a guess or hint is sent to a round that already has an answer. */
public class RoundClosedException extends RuntimeException {

  public RoundClosedException(int roundNumber) {
    super(
        "Round %d is already answered, so it can't take another guess or hint"
            .formatted(roundNumber));
  }
}
