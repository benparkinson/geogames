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
import java.time.temporal.ChronoUnit;
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

  /**
   * When the game was created, rounded down to the hour. It is only used to expire old games, so
   * it doesn't need to be exact, and an exact time could be matched against a request log to tie a
   * game to a client. A game is therefore deleted up to an hour before its maximum age.
   */
  @Column(nullable = false)
  private Instant createdAt;

  /**
   * Random per game, shared by the rows the usage statistics record for its rounds. It is not the
   * public id, which is in URLs and request logs, and it is never sent to a client or logged. Null
   * for a game created before the statistics existed.
   */
  @Column(updatable = false)
  private UUID playId;

  /** What the player asked for: a {@code GameType} name, or {@code RANDOM}. Null for old games. */
  @Column(updatable = false)
  private String gameMode;

  public GameEntity() {}

  /**
   * A game always has at least one round, so its result is never "finished" with nothing played.
   *
   * @param gameMode what was requested: a {@code GameType} name, or {@code RANDOM}
   */
  public GameEntity(List<GameRoundEntity> rounds, String gameMode) {
    if (rounds == null || rounds.isEmpty()) {
      throw new IllegalArgumentException("A game needs at least one round");
    }
    this.rounds = rounds;
    this.createdAt = Instant.now().truncatedTo(ChronoUnit.HOURS);
    // UUID.randomUUID() uses SecureRandom: 122 random bits, so it can't be guessed.
    this.publicId = UUID.randomUUID();
    this.playId = UUID.randomUUID();
    this.gameMode = gameMode;
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

  public UUID getPlayId() {
    return playId;
  }

  public String getGameMode() {
    return gameMode;
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

  /** Deliberately leaves out the play id. */
  @Override
  public String toString() {
    return "GameEntity[" + "id=" + id + "]";
  }
}
