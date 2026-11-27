package com.parkinson.benjamin.geogames.api;

import java.util.List;
import java.util.UUID;

/**
 * The league table as one game sees it.
 *
 * @param challengeId the challenge's public id
 * @param creator whether this game made the challenge
 * @param entries the top of the table
 * @param yours this game's own row when it is below {@code entries}, otherwise null
 * @param canEnterInitials whether this game's row is in {@code entries}
 */
public record ChallengeTableResponse(
    UUID challengeId,
    int maxScore,
    int entryCount,
    boolean creator,
    List<ChallengeEntryView> entries,
    ChallengeEntryView yours,
    boolean canEnterInitials) {}
