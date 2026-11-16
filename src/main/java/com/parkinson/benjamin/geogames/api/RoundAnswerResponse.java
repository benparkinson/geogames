package com.parkinson.benjamin.geogames.api;

import com.parkinson.benjamin.geogames.model.AnswerState;

/**
 * The response to an answer: the round's answer and points, and the whole game's result. It has no
 * round data, which the client already holds. The field names match {@link GameRound}'s.
 */
public record RoundAnswerResponse(
    AnswerState answerState, int hintsUsed, int points, GameResult gameResult) {}
