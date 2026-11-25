package com.parkinson.benjamin.geogames.api;

/**
 * Thrown for a request that breaks a rule (an unknown name, a number out of range, a missing
 * field). It is answered with a 400, and the message goes to the client, so it must not reveal
 * anything internal. Any other {@link IllegalArgumentException} is a server bug and a 500.
 */
public class BadRequestException extends IllegalArgumentException {

  public BadRequestException(String message) {
    super(message);
  }
}
