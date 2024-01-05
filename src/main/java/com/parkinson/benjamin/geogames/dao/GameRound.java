package com.parkinson.benjamin.geogames.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.Objects;

@Entity

public final class GameRound {
  @Id
  @GeneratedValue
  private int id;

  private int index;
  @Lob
  @Column
  private String jsonBlob;

  public GameRound() {
  }

  public GameRound(int index, String jsonBlob) {
    this.index = index;
    this.jsonBlob = jsonBlob;
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

  public String getJsonBlob() {
    return jsonBlob;
  }

  public void setJsonBlob(String jsonBlob) {
    this.jsonBlob = jsonBlob;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GameRound gameRound = (GameRound) o;
    return id == gameRound.id && Objects.equals(jsonBlob, gameRound.jsonBlob);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, jsonBlob);
  }
}
