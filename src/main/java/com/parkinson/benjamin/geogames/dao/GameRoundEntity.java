package com.parkinson.benjamin.geogames.dao;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * One round of a game. Not final, so Hibernate can make lazy proxies of it, and it has no {@code
 * equals}: two rounds are the same only if they are the same object.
 */
@Entity
public class GameRoundEntity {
  @Id @GeneratedValue private long id;

  /** Where this round comes in its game, counting from 0. */
  private int roundNumber;

  @Enumerated(EnumType.STRING)
  private GameType gameType;

  @Lob @Column private String jsonBlob;

  private AnswerState answerState;

  /** How many hints the player used before answering. */
  private int hintsUsed;

  public GameRoundEntity() {}

  public GameRoundEntity(int roundNumber, GameType gameType, String jsonBlob) {
    this.roundNumber = roundNumber;
    this.gameType = gameType;
    this.jsonBlob = jsonBlob;
    this.answerState = AnswerState.UNANSWERED;
    this.hintsUsed = 0;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public int getRoundNumber() {
    return roundNumber;
  }

  public void setRoundNumber(int roundNumber) {
    this.roundNumber = roundNumber;
  }

  public GameType getGameType() {
    return gameType;
  }

  public void setGameType(GameType gameType) {
    this.gameType = gameType;
  }

  public String getJsonBlob() {
    return jsonBlob;
  }

  public void setJsonBlob(String jsonBlob) {
    this.jsonBlob = jsonBlob;
  }

  public AnswerState getAnswerState() {
    return answerState;
  }

  public void setAnswerState(AnswerState answerState) {
    this.answerState = answerState;
  }

  public int getHintsUsed() {
    return hintsUsed;
  }

  public void setHintsUsed(int hintsUsed) {
    this.hintsUsed = hintsUsed;
  }
}
