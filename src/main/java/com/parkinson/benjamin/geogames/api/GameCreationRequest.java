package com.parkinson.benjamin.geogames.api;

/**
 * @param difficulty {@code EASY}, {@code NORMAL} or {@code HARD}, or null; wins over {@code
 *     hardMode} when both are sent
 * @param hardMode the old Hard mode switch, from a client that doesn't send {@code difficulty};
 *     null from a client that sent neither, which is stored as unknown
 */
public record GameCreationRequest(
    String gameType, int numberOfRounds, String difficulty, Boolean hardMode) {}
