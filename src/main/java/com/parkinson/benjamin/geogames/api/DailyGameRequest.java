package com.parkinson.benjamin.geogames.api;

/**
 * The player's local date as {@code YYYY-MM-DD}, or null to use the server's UTC date. A string,
 * not a {@code LocalDate}, so a malformed date is our 400 and not a JSON parse error.
 */
public record DailyGameRequest(String date) {}
