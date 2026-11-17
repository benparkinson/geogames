package com.parkinson.benjamin.geogames.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * One guess made in a round: a known place name (the spelling from the name lists, never the text
 * as typed) and whether the client found it to be an answer.
 */
@Embeddable
public class RoundGuess {

  @Column(name = "guess", nullable = false, length = 255)
  private String guess;

  @Column(name = "correct", nullable = false)
  private boolean correct;

  public RoundGuess() {}

  public RoundGuess(String guess, boolean correct) {
    this.guess = guess;
    this.correct = correct;
  }

  public String getGuess() {
    return guess;
  }

  public boolean isCorrect() {
    return correct;
  }
}
