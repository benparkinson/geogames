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
public final class GameEntity {

  @Id
  @GeneratedValue
  private long id;
  private GameType name;
  @Lob
  @Column
  @OneToMany(cascade = CascadeType.ALL)
  private List<GameRoundEntity> rounds;

  public GameEntity() {
  }

  public GameEntity(GameType name, List<GameRoundEntity> rounds) {
    this.name = name;
    this.rounds = rounds;
  }

  public GameEntity(long id, GameType name) {
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

  public List<GameRoundEntity> getRounds() {
    return rounds;
  }

  public void setRounds(List<GameRoundEntity> rounds) {
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
    var that = (GameEntity) obj;
    return this.id == that.id &&
        Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "GameEntity[" +
        "id=" + id + ", " +
        "name=" + name + ']';
  }


}
