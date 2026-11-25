package com.parkinson.benjamin.geogames.api;

import java.time.LocalDate;
import java.util.List;

/** A stored daily challenge's game types, in round order. Never the rounds themselves. */
public record DailyChallengeResponse(LocalDate date, long number, List<String> gameTypes) {}
