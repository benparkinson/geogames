package com.parkinson.benjamin.geogames.dao;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One round of a game. Not final, so Hibernate can make lazy proxies of it, and it has no {@code
 * equals}: two rounds are the same only if they are the same object.
 */
@Entity
public class GameRoundEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  /** Where this round comes in its game, counting from 0. */
  private int roundNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 64)
  private GameType gameType;

  /**
   * Names this round's data (see {@code GameData.roundKey()}). The server rebuilds the JSON from
   * its cached map data on every read, so the geometry isn't stored a second time.
   */
  @Column(nullable = false, length = 255)
  private String roundKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AnswerState answerState;

  /** How many hints the player used before answering. */
  private int hintsUsed;

  /**
   * Wrong guesses the client reported, as counted against the guess limit. Null if the client
   * didn't report it.
   */
  private Integer wrongGuesses;

  /**
   * The guesses made so far, in order. A change to this collection bumps the round's version, so
   * two guesses at the same moment conflict like two answers do.
   */
  @ElementCollection
  @CollectionTable(name = "game_round_guess", joinColumns = @JoinColumn(name = "round_id"))
  @OrderColumn(name = "position")
  private List<RoundGuess> guesses = new ArrayList<>();

  /**
   * For each hint taken before the round was answered, how many guesses the round had when it was
   * taken (entry i is hint i). A change bumps the round's version, like the guesses.
   */
  @ElementCollection
  @CollectionTable(name = "game_round_hint", joinColumns = @JoinColumn(name = "round_id"))
  @OrderColumn(name = "hint_index")
  @Column(name = "guesses_before", nullable = false)
  private List<Integer> hintsTakenAt = new ArrayList<>();

  /** Optimistic lock: a second writer of the same round fails rather than overwriting the first. */
  @Version
  @Column(nullable = false)
  private int version;

  public GameRoundEntity() {}

  public GameRoundEntity(int roundNumber, GameType gameType, String roundKey) {
    this.roundNumber = roundNumber;
    this.gameType = gameType;
    this.roundKey = roundKey;
    this.answerState = AnswerState.UNANSWERED;
    this.hintsUsed = 0;
    this.wrongGuesses = null;
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

  public String getRoundKey() {
    return roundKey;
  }

  public void setRoundKey(String roundKey) {
    this.roundKey = roundKey;
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

  public Integer getWrongGuesses() {
    return wrongGuesses;
  }

  public void setWrongGuesses(Integer wrongGuesses) {
    this.wrongGuesses = wrongGuesses;
  }

  public int getVersion() {
    return version;
  }

  public List<RoundGuess> getGuesses() {
    return Collections.unmodifiableList(guesses);
  }

  public void addGuess(RoundGuess guess) {
    guesses.add(guess);
  }

  public List<Integer> getHintsTakenAt() {
    return Collections.unmodifiableList(hintsTakenAt);
  }

  public void addHint(int guessesBefore) {
    hintsTakenAt.add(guessesBefore);
  }
}
