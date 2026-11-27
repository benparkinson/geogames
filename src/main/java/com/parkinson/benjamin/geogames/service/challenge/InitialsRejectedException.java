package com.parkinson.benjamin.geogames.service.challenge;

import com.parkinson.benjamin.geogames.api.BadRequestException;

/** A 400 for initials {@link Initials} won't take, saying why for the metrics. */
public class InitialsRejectedException extends BadRequestException {

  /** Why. */
  public enum Problem {
    INVALID,
    BLOCKED
  }

  private final Problem problem;

  public InitialsRejectedException(Problem problem, String message) {
    super(message);
    this.problem = problem;
  }

  public Problem problem() {
    return problem;
  }
}
