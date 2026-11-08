package com.parkinson.benjamin.geogames.model;

public enum AnswerState {
  /** The round hasn't been finished yet. */
  UNANSWERED,
  /** The player named every answer. */
  CORRECT,
  /** The player gave up, so the answers were revealed. */
  GAVE_UP,
  /** The player used up all the wrong guesses the round allows, so the answers were revealed. */
  OUT_OF_GUESSES
}
