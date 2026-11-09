package com.parkinson.benjamin.geogames.model.stats;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One anonymous, flat, logical row (the same columns as the {@code round_result} view): how one
 * answered round went. {@code RoundStatsStore} splits it across the {@code stat_*} tables. It holds
 * neither game id, and no time of day. {@code wrongGuesses} is null when the client didn't report
 * it, which is not the same as 0.
 */
public record RoundResult(
    LocalDate answeredOn,
    UUID playId,
    String gameMode,
    int roundsInGame,
    int roundNumber,
    String gameType,
    String roundKey,
    String answerState,
    int hintsUsed,
    Integer wrongGuesses,
    int hintCount,
    int guessLimit,
    int points) {}
