package com.parkinson.benjamin.geogames.api;

/**
 * @param difficulty {@code EASY}, {@code NORMAL} or {@code HARD}, or null, which is stored as
 *     unknown
 */
public record GameCreationRequest(String gameType, int numberOfRounds, String difficulty) {}
