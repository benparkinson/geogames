package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.AnswerState;

/** Thrown when a round that already has an answer is sent a different one. */
public class RoundAlreadyAnsweredException extends RuntimeException {

  public RoundAlreadyAnsweredException(
      int roundNumber,
      AnswerState current,
      int currentHintsUsed,
      Integer currentWrongGuesses,
      Integer currentAnswersFound,
      AnswerState requested,
      int requestedHintsUsed,
      Integer requestedWrongGuesses,
      Integer requestedAnswersFound) {
    super(
        ("Round %d was already answered %s with %d hints, %s wrong guesses and %s answers found and"
                + " can't be changed to %s with %d hints, %s wrong guesses and %s answers found")
            .formatted(
                roundNumber,
                current,
                currentHintsUsed,
                describe(currentWrongGuesses),
                describe(currentAnswersFound),
                requested,
                requestedHintsUsed,
                describe(requestedWrongGuesses),
                describe(requestedAnswersFound)));
  }

  private static String describe(Integer count) {
    return count == null ? "unreported" : count.toString();
  }
}
