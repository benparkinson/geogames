package com.parkinson.benjamin.geogames.service.challenge;

/** The game isn't finished yet, so it can't be challenged. Answered with a 409. */
public class ChallengeConflictException extends RuntimeException {

  public ChallengeConflictException(String message) {
    super(message);
  }
}
