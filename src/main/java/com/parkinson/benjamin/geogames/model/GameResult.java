package com.parkinson.benjamin.geogames.model;

import java.util.List;

public record GameResult(
    GameState gameState, int roundsUnanswered, int score, List<AnswerState> roundAnswerStates) {}
