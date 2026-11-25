package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameState;
import java.util.List;

/**
 * Where a game stands. {@code score} is the sum of the round points out of {@code maxScore}. {@code
 * roundsCorrect} is how many rounds were answered correctly, whatever the hints. The lists are
 * ordered by round number. {@code roundFirstGuess} says for each round whether it was correct with
 * no hints and no wrong guess, and {@code firstGuessRounds} counts them.
 */
public record GameResult(
    GameState gameState,
    int roundsUnanswered,
    int score,
    List<AnswerState> roundAnswerStates,
    int maxScore,
    int roundsCorrect,
    List<Integer> roundPoints,
    List<Integer> roundHintsUsed,
    List<Boolean> roundFirstGuess,
    int firstGuessRounds) {}
