package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.AnswerState;

/** Thrown when a round that already has an answer is sent a different one. */
public class RoundAlreadyAnsweredException extends RuntimeException {

  public RoundAlreadyAnsweredException(
      int roundNumber,
      AnswerState current,
      int currentHintsUsed,
      AnswerState requested,
      int requestedHintsUsed) {
    super(
        "Round %d was already answered %s with %d hints and can't be changed to %s with %d hints"
            .formatted(roundNumber, current, currentHintsUsed, requested, requestedHintsUsed));
  }
}
