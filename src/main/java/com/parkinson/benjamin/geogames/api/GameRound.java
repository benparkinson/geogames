package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.AnswerState;
import java.util.List;

public record GameRound(
    GameTypeResponse gameType,
    String jsonBlob,
    int totalRoundCount,
    AnswerState answerState,
    int hintsUsed,
    int points,
    GameResult gameResult,
    List<RecordedGuess> guesses,
    List<Integer> hintsTakenAt) {}
