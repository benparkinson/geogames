package com.parkinson.benjamin.geogames.dao;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import java.util.List;
import java.util.Objects;

@Entity
public final class Game {

  @Id
  @GeneratedValue
  private long id;
  private GameType name;
  @Lob
  @Column
  @OneToMany(cascade = CascadeType.ALL)
  private List<GameRound> rounds;

  public Game() {
  }

  public Game(GameType name, List<GameRound> rounds) {
    this.name = name;
    this.rounds = rounds;
  }

  public Game(long id, GameType name) {
    this.id = id;
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public GameType getName() {
    return name;
  }

  public void setName(GameType name) {
    this.name = name;
  }

  public List<GameRound> getRounds() {
    return rounds;
  }

  public void setRounds(List<GameRound> rounds) {
    this.rounds = rounds;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (Game) obj;
    return this.id == that.id &&
        Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "Game[" +
        "id=" + id + ", " +
        "name=" + name + ']';
  }


}
