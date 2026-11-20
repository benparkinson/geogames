package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.AnswerState;

/**
 * The answer to a round. {@code hintsUsed} is an {@code Integer} so that a client that doesn't send
 * it (an older one) is treated as having used no hints. {@code wrongGuesses} is an {@code Integer}
 * too, but a missing value is not treated as 0: it is stored as unknown ({@code null}), so "no
 * wrong guesses" can be told apart from "an old client didn't say". {@code answersFound} is how
 * many of the round's answers were found; a missing value (an older client) is unknown, and is
 * stored as {@code null} unless correct guesses were recorded for the round.
 */
public record GameRoundAnswerRequest(
    AnswerState answerState, Integer hintsUsed, Integer wrongGuesses, Integer answersFound) {

  public int hintsUsedOrZero() {
    return hintsUsed == null ? 0 : hintsUsed;
  }
}
