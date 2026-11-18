package com.parkinson.benjamin.geogames.api;

import java.time.LocalDate;
import java.util.UUID;

/** A new game made from the day's pinned rounds: {@code id} is the game's public id. */
public record DailyGameResponse(UUID id, LocalDate date, long number) {}
