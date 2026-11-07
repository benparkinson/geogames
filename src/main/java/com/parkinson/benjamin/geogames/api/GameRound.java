package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.AnswerState;

public record GameRound(
    GameTypeResponse gameType,
    String jsonBlob,
    int totalRoundCount,
    AnswerState answerState,
    GameResult gameResult) {}
