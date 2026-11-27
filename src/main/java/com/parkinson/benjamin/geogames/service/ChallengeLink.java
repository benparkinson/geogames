package com.parkinson.benjamin.geogames.service;

/**
 * A game's links to its challenge.
 *
 * @param challengeId the challenge the game made or was started from, or null
 * @param entryId the game's entry in it, or null until the game is finished
 * @param creator whether the game made the challenge, rather than being started from it
 */
public record ChallengeLink(Long challengeId, Long entryId, boolean creator) {}
