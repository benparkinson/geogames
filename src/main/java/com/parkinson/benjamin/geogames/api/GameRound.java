package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.AnswerState;
import java.util.List;

/**
 * @param difficulty always {@code EASY}, {@code NORMAL} or {@code HARD} (unknown reads as NORMAL)
 * @param continents the continent names to show from the start of the round: empty unless the game
 *     is Easy
 */
public record GameRound(
    GameTypeResponse gameType,
    String jsonBlob,
    int totalRoundCount,
    AnswerState answerState,
    int hintsUsed,
    int points,
    GameResult gameResult,
    List<RecordedGuess> guesses,
    List<Integer> hintsTakenAt,
    String difficulty,
    List<String> continents) {}
