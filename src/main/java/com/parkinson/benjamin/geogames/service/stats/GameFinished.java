package com.parkinson.benjamin.geogames.service.stats;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.model.GameOrigin;

/**
 * Published when a game's last round is first answered, and handled after the transaction commits.
 * It holds no ids.
 *
 * @param difficulty null when unknown
 * @param challengeScore the score to beat for a game started from a challenge, otherwise null
 */
public record GameFinished(
    GameOrigin origin, Difficulty difficulty, int score, int maxScore, Integer challengeScore) {}
