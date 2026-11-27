package com.parkinson.benjamin.geogames.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One row of a challenge's league table.
 *
 * @param initials null until entered
 * @param you whether this is the asking game's row; left out of the public challenge page
 */
public record ChallengeEntryView(
    int rank, int score, String initials, @JsonInclude(JsonInclude.Include.NON_NULL) Boolean you) {}
