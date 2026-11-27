package com.parkinson.benjamin.geogames.service.challenge;

import com.parkinson.benjamin.geogames.model.Difficulty;
import com.parkinson.benjamin.geogames.service.daily.PinnedRound;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A stored challenge. */
public record Challenge(
    UUID publicId,
    String gameMode,
    Difficulty difficulty,
    List<PinnedRound> rounds,
    int maxScore,
    LocalDate createdOn,
    LocalDate lastUsedOn) {}
