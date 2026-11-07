package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.AnswerState;

/** Thrown when a round that already has an answer is sent a different one. */
public class RoundAlreadyAnsweredException extends RuntimeException {

  public RoundAlreadyAnsweredException(
      int roundNumber, AnswerState current, AnswerState requested) {
    super(
        "Round %d was already answered %s and can't be changed to %s"
            .formatted(roundNumber, current, requested));
  }
}
