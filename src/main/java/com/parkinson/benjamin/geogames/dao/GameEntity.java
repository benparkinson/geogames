package com.parkinson.benjamin.geogames.dao;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameOrigin;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.Instant;
import java.time.LocalDate;
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
   * public id, which is in URLs and request logs, and it is never sent to a client or logged.
   */
  @Column(nullable = false, updatable = false)
  private UUID playId;

  /** What the player asked for: a {@code GameType} name, or {@code RANDOM}. */
  @Column(nullable = false, updatable = false)
  private String gameMode;

  /**
   * How the game is played, chosen when it was created and never changed. Null means unknown: the
   * game was created before this column existed, or by a client that sent neither a difficulty nor
   * Hard mode. It reads as NORMAL in the API.
   */
  @Column(updatable = false)
  @Enumerated(EnumType.STRING)
  private Difficulty difficulty;

  /**
   * The date of the daily challenge this game was made from, set when it is created and never
   * changed. Null for every other game. It is the challenge's date, not when it was played.
   */
  @Column(updatable = false)
  private LocalDate dailyDate;

  /**
   * The score to beat, for a game started from a challenge: set when it is created and never
   * changed. Null for every other game.
   */
  @Column(updatable = false)
  private Integer challengeScore;

  /**
   * The challenge this game made or was started from, or null. The database nulls it when the
   * challenge expires.
   */
  private Long challengeId;

  /** This game's entry in that challenge; null until the game is finished. */
  private Long challengeEntryId;

  public GameEntity() {}

  /** A game that is not a daily challenge. */
  public GameEntity(List<GameRoundEntity> rounds, String gameMode, Difficulty difficulty) {
    this(rounds, gameMode, difficulty, null);
  }

  /** A game that was not started from a challenge. */
  public GameEntity(
      List<GameRoundEntity> rounds, String gameMode, Difficulty difficulty, LocalDate dailyDate) {
    this(rounds, gameMode, difficulty, dailyDate, null, null);
  }

  /**
   * A game always has at least one round, so its result is never "finished" with nothing played.
   *
   * @param gameMode what was requested: a {@code GameType} name, or {@code RANDOM}
   * @param difficulty how the game is played, or null if unknown
   * @param dailyDate the challenge's date for a daily game, or null
   * @param challengeScore the score to beat for a game started from a challenge, or null
   * @param challengeId the challenge a game was started from, or null
   */
  public GameEntity(
      List<GameRoundEntity> rounds,
      String gameMode,
      Difficulty difficulty,
      LocalDate dailyDate,
      Integer challengeScore,
      Long challengeId) {
    if (rounds == null || rounds.isEmpty()) {
      throw new IllegalArgumentException("A game needs at least one round");
    }
    if (gameMode == null) {
      throw new IllegalArgumentException("A game needs a mode");
    }
    this.rounds = rounds;
    this.createdAt = Instant.now().truncatedTo(ChronoUnit.HOURS);
    // UUID.randomUUID() uses SecureRandom: 122 random bits, so it can't be guessed.
    this.publicId = UUID.randomUUID();
    this.playId = UUID.randomUUID();
    this.gameMode = gameMode;
    this.difficulty = difficulty;
    this.dailyDate = dailyDate;
    this.challengeScore = challengeScore;
    this.challengeId = challengeId;
  }

  /** Records the challenge this game made or was started from, and its entry there. */
  public void linkChallenge(long challengeId, long entryId) {
    this.challengeId = challengeId;
    this.challengeEntryId = entryId;
  }

  public Long getChallengeId() {
    return challengeId;
  }

  public Long getChallengeEntryId() {
    return challengeEntryId;
  }

  /** The daily challenge's date, or null for a game that isn't one. */
  public LocalDate getDailyDate() {
    return dailyDate;
  }

  /** The score to beat, or null for a game that wasn't started from a challenge. */
  public Integer getChallengeScore() {
    return challengeScore;
  }

  /** Derived from the daily date and the challenge score, not stored. */
  public GameOrigin origin() {
    if (dailyDate != null) {
      return GameOrigin.DAILY;
    }
    return challengeScore != null ? GameOrigin.CHALLENGE : GameOrigin.FREE_PLAY;
  }

  public long getId() {
    return id;
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

  /** For statistics: null when unknown. */
  public Difficulty getDifficulty() {
    return difficulty;
  }

  /** For the rules and the API: unknown reads as NORMAL. */
  public Difficulty difficulty() {
    return Difficulty.orNormal(difficulty);
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
