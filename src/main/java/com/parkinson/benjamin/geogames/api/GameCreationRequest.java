package com.parkinson.benjamin.geogames.api;

/**
 * @param hardMode whether Hard mode is on for the whole game; null from an old client that did not
 *     send it, which is stored as unknown
 */
public record GameCreationRequest(String gameType, int numberOfRounds, Boolean hardMode) {}
