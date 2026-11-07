package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.AnswerState;
import com.parkinson.benjamin.geogames.model.GameState;
import java.util.List;

public record GameResult(
    GameState gameState, int roundsUnanswered, int score, List<AnswerState> roundAnswerStates) {}
