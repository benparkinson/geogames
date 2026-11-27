package com.parkinson.benjamin.geogames.service.challenge;

/** The game's state doesn't allow it (unfinished, or too low in the table). A 409. */
public class ChallengeConflictException extends RuntimeException {

  public ChallengeConflictException(String message) {
    super(message);
  }
}
