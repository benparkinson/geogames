package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.AnswerState;

/**
 * The answer to a round. {@code hintsUsed} is an {@code Integer} so that a client that doesn't send
 * it (an older one) is treated as having used no hints.
 */
public record GameRoundAnswerRequest(AnswerState answerState, Integer hintsUsed) {

  public int hintsUsedOrZero() {
    return hintsUsed == null ? 0 : hintsUsed;
  }
}
