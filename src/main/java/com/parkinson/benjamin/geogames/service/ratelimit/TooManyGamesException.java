package com.parkinson.benjamin.geogames.service.ratelimit;

/** A client has started more games than its window allows. Carries the wait, never the address. */
public class TooManyGamesException extends RuntimeException {

  private final long retryAfterSeconds;

  public TooManyGamesException(long retryAfterSeconds) {
    super("Too many new games");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
