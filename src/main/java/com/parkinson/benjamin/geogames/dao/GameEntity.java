package com.parkinson.benjamin.geogames.dao;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.List;

/**
 * A game and its rounds. Not final, so Hibernate can make lazy proxies of it, and it has no {@code
 * equals}: two games are the same only if they are the same object.
 */
@Entity
public class GameEntity {

  @Id @GeneratedValue private long id;

  @OneToMany(cascade = CascadeType.ALL)
  @OrderBy("roundNumber")
  private List<GameRoundEntity> rounds;

  public GameEntity() {}

  public GameEntity(List<GameRoundEntity> rounds) {
    this.rounds = rounds;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public List<GameRoundEntity> getRounds() {
    return rounds;
  }

  public void setRounds(List<GameRoundEntity> rounds) {
    this.rounds = rounds;
  }

  @Override
  public String toString() {
    return "GameEntity[" + "id=" + id + "]";
  }
}
