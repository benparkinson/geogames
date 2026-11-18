package com.parkinson.benjamin.geogames.api;

/**
 * The player's local date as {@code YYYY-MM-DD}, or null to use the server's UTC date. A string,
 * not a {@code LocalDate}, so a malformed date is our 400 and not a JSON parse error.
 *
 * <p>{@code hardMode} is whether Hard mode is on for the game; null from an old client, which is
 * stored as unknown.
 */
public record DailyGameRequest(String date, Boolean hardMode) {}
