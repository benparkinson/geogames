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

  /** A game always has at least one round, so its result is never "finished" with nothing played. */
  public GameEntity(List<GameRoundEntity> rounds) {
    if (rounds == null || rounds.isEmpty()) {
      throw new IllegalArgumentException("A game needs at least one round");
    }
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

  @Override
  public String toString() {
    return "GameEntity[" + "id=" + id + "]";
  }
}
