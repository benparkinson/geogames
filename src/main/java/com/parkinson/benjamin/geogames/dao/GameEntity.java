package com.parkinson.benjamin.geogames.dao;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A game and its rounds. It has two ids. The {@code long} {@link #getId() id} is the primary key
 * and never leaves the server. The {@link #getPublicId() public id} is a random UUID: it is the id
 * in the API and the URLs, so a player can't open another player's game by counting.
 *
 * <p>Not final, so Hibernate can make lazy proxies of it, and it has no {@code
 * equals}: two games are the same only if they are the same object.
 */
@Entity
public class GameEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "game_id", nullable = false)
  @OrderBy("roundNumber")
  private List<GameRoundEntity> rounds;

  @Column(nullable = false, unique = true, updatable = false)
  private UUID publicId;

  @Column(nullable = false)
  private Instant createdAt;

  public GameEntity() {}

  /** A game always has at least one round, so its result is never "finished" with nothing played. */
  public GameEntity(List<GameRoundEntity> rounds) {
    if (rounds == null || rounds.isEmpty()) {
      throw new IllegalArgumentException("A game needs at least one round");
    }
    this.rounds = rounds;
    this.createdAt = Instant.now();
    // UUID.randomUUID() uses SecureRandom: 122 random bits, so it can't be guessed.
    this.publicId = UUID.randomUUID();
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public List<GameRoundEntity> getRounds() {
    return rounds;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "GameEntity[" + "id=" + id + "]";
  }
}
