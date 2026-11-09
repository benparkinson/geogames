package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.AnswerState;

/** Thrown when a round that already has an answer is sent a different one. */
public class RoundAlreadyAnsweredException extends RuntimeException {

  public RoundAlreadyAnsweredException(
      int roundNumber,
      AnswerState current,
      int currentHintsUsed,
      Integer currentWrongGuesses,
      AnswerState requested,
      int requestedHintsUsed,
      Integer requestedWrongGuesses) {
    super(
        ("Round %d was already answered %s with %d hints and %s wrong guesses and can't be changed"
                + " to %s with %d hints and %s wrong guesses")
            .formatted(
                roundNumber,
                current,
                currentHintsUsed,
                describe(currentWrongGuesses),
                requested,
                requestedHintsUsed,
                describe(requestedWrongGuesses)));
  }

  private static String describe(Integer wrongGuesses) {
    return wrongGuesses == null ? "unreported" : wrongGuesses.toString();
  }
}
