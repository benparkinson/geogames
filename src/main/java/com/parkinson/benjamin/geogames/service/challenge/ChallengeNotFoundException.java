package com.parkinson.benjamin.geogames.service.challenge;

/** The game or challenge doesn't exist (or has expired). Answered with a 404. */
public class ChallengeNotFoundException extends RuntimeException {

  public ChallengeNotFoundException(String message) {
    super(message);
  }
}
