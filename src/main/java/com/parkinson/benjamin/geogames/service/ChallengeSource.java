package com.parkinson.benjamin.geogames.service;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import java.util.List;

/**
 * A finished, non-daily game, as a challenge copies it: the rounds, settings and score.
 *
 * @param challengeId the challenge the game made or was started from, or null
 */
public record ChallengeSource(
    List<PinnedRound> rounds,
    String gameMode,
    Difficulty difficulty,
    int score,
    int maxScore,
    Long challengeId) {}
