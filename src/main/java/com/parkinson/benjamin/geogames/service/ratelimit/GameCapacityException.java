package com.parkinson.benjamin.geogames.service.ratelimit;

/** The database holds as many live games as the global cap allows. */
public class GameCapacityException extends RuntimeException {

  private final long retryAfterSeconds;

  public GameCapacityException(long retryAfterSeconds) {
    super("Game capacity reached");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
