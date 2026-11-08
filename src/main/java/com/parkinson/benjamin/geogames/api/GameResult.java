package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameState;
import java.util.List;

/**
 * Where a game stands. {@code score} is the sum of the round points out of {@code maxScore}. {@code
 * roundsCorrect} is how many rounds were answered correctly, whatever the hints. The lists are
 * ordered by round number.
 */
public record GameResult(
    GameState gameState,
    int roundsUnanswered,
    int score,
    List<AnswerState> roundAnswerStates,
    int maxScore,
    int roundsCorrect,
    int perfectRounds,
    List<Integer> roundPoints,
    List<Integer> roundHintsUsed) {}
