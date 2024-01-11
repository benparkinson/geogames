package com.parkinson.benjamin.geogames.dao;

import com.parkinson.benjamin.geogames.model.AnswerState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.Objects;

@Entity
public final class GameRoundEntity {
  @Id @GeneratedValue private int id;

  private int index;

  private GameType gameType;

  @Lob @Column private String jsonBlob;

  private AnswerState answerState;

  public GameRoundEntity() {}

  public GameRoundEntity(int index, GameType gameType, String jsonBlob) {
    this.index = index;
    this.gameType = gameType;
    this.jsonBlob = jsonBlob;
    this.answerState = AnswerState.UNANSWERED;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GameRoundEntity gameRoundEntity = (GameRoundEntity) o;
    return id == gameRoundEntity.id && gameType == gameRoundEntity.gameType && Objects.equals(jsonBlob, gameRoundEntity.jsonBlob);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, gameType, jsonBlob);
  }
}
