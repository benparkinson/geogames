package com.parkinson.benjamin.geogames.api;

/** What a friend sees before playing a challenge: never the rounds or their answers. */
public record ChallengeResponse(
    String gameMode, String difficulty, int roundCount, int score, int maxScore) {}
