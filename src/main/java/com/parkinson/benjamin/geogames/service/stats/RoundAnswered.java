package com.parkinson.benjamin.geogames.service.stats;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Published by {@code GameService} when a round is answered for the first time. It holds plain
 * values only, because it is handled after the transaction has committed, when the entities' lazy
 * collections can't be read. It deliberately has no game id. Its {@code toString} would print the
 * play id, so it must never be logged.
 */
public record RoundAnswered(
    UUID playId,
    String gameMode,
    int roundsInGame,
    int roundNumber,
    GameType gameType,
    String roundKey,
    AnswerState answerState,
    int hintsUsed,
    Integer wrongGuesses,
    LocalDate answeredOn) {}
