package com.parkinson.benjamin.geogames.service.ratelimit;

/** A client has made more API requests than its window allows. Carries the wait, never the address. */
public class TooManyRequestsException extends RuntimeException {

  private final long retryAfterSeconds;

  public TooManyRequestsException(long retryAfterSeconds) {
    super("Too many requests");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
